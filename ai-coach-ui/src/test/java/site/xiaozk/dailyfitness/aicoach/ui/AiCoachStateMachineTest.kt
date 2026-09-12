package site.xiaozk.dailyfitness.aicoach.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.engine.AiCoachResult
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessageContent
import site.xiaozk.dailyfitness.aicoach.engine.IAiCoach
import site.xiaozk.dailyfitness.repository.IAiCoachConfigStore
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig

/**
 * M2.7: the state machine must keep the **full** conversation in [AiCoachUiState]
 * (rendered by the LazyColumn) and hand the untrimmed list to the engine, which is
 * the single place applying the 5-round/10-message request cap.
 *
 * M3.2: with the inline config form removed, `ConfigMissing` must react to the
 * settings page storing a key and move to `Idle` on its own.
 */
class AiCoachStateMachineTest {

    @Test
    fun `the real reply reuses the pending bubble id`() = runTest {
        val store = FakeConfigStore(AiCoachConfig(apiKey = "test-key"))
        val provider = AiCoachConfigProvider(store)
        provider.config.first { it.configured }

        // Hold the request open so the Loading state is observable.
        val gate = CompletableDeferred<Unit>()
        val coach = object : IAiCoach {
            override suspend fun recommendToday(history: List<CoachMessage>): AiCoachResult {
                gate.await()
                return AiCoachResult.TodayPlan(
                    sessionsUsed = 0,
                    rounds = 1,
                    parts = emptyList(),
                    ignoredNames = emptyList(),
                    assistantMessage = CoachMessage(
                        fromUser = false,
                        content = CoachMessageContent.PlanSummary(emptyList()),
                    ),
                )
            }
        }
        val machine = AiCoachStateMachine(coach, provider).launchIn(backgroundScope)
        machine.state.first { it is AiCoachUiState.Idle }

        val userContent = CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1))
        machine.dispatchAction(AiCoachUiAction.Refresh(userContent))
        val loading = machine.state.first { it is AiCoachUiState.Loading } as AiCoachUiState.Loading
        assertEquals(userContent, (loading.history[loading.requestHistory.size]).content)
        val pendingId = loading.history.last().id
        assertTrue(loading.history.last().isLoading)

        gate.complete(Unit)
        val idle = machine.state.first { it is AiCoachUiState.Idle && it.history.size == 2 }
            as AiCoachUiState.Idle
        val reply = idle.history.last()
        assertFalse(reply.isLoading)
        // Same list key => LazyColumn reuses the item and the loading bubble morphs in place.
        assertEquals(pendingId, reply.id)
    }

    @Test
    fun `Loading appends a locally-built user bubble and a pending assistant bubble`() {
        val raw = listOf(
            CoachMessage(
                fromUser = true,
                content = CoachMessageContent.AdviceRequest(LocalDate(2025, 1, 1), setsToday = 2, partName = "胸部"),
            ),
            CoachMessage(
                fromUser = false,
                content = CoachMessageContent.PlanSummary(emptyList()),
            ),
        )
        val userContent = CoachMessageContent.PlanRequest(LocalDate(2025, 1, 2))
        val loading = AiCoachUiState.Loading(
            setsToday = 3,
            requestHistory = raw,
            pendingUserContent = userContent,
        )

        // The raw request history stays untouched (it is what the engine receives)...
        assertEquals(raw, loading.requestHistory)
        // ...while the rendered history adds the user turn *then* the loading bubble.
        assertEquals(raw.size + 2, loading.history.size)
        assertEquals(raw, loading.history.dropLast(2))
        val pendingUser = loading.history[raw.size]
        assertTrue(pendingUser.fromUser)
        assertEquals(userContent, pendingUser.content)
        val pendingAssistant = loading.history.last()
        assertFalse(pendingAssistant.fromUser)
        assertTrue(pendingAssistant.isLoading)
        // Stable identity across accesses, so the bubble can recompose in place.
        assertSame(pendingAssistant, loading.history.last())
    }

    @Test
    fun `keeps the full conversation across turns`() = runTest {
        val store = FakeConfigStore(AiCoachConfig(apiKey = "test-key"))
        val provider = AiCoachConfigProvider(store)
        provider.config.first { it.configured }

        val coach = RecordingCoach()
        val machine = AiCoachStateMachine(coach, provider).launchIn(backgroundScope)

        // Initial config probe -> Idle.
        machine.state.first { it is AiCoachUiState.Idle }

        val turns = 7 // 14 messages, beyond the 5-round (10-message) request window.
        val userContent = CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1))
        repeat(turns) { index ->
            machine.dispatchAction(AiCoachUiAction.Refresh(userContent))
            machine.state.first { it is AiCoachUiState.Idle && coach.histories.size == index + 1 }
        }

        val idle = machine.state.value as AiCoachUiState.Idle
        // UI state keeps everything...
        assertEquals(turns * 2, idle.history.size)
        // ...and each request received the full history so far (0, 2, 4, ... 12),
        // proving the machine no longer truncates before calling the engine.
        assertEquals((0 until turns).map { it * 2 }, coach.histories.map { it.size })
        assertTrue(coach.histories.last().size > 10)
    }

    @Test
    fun `ConfigMissing moves to Idle once the settings page stores a key`() = runTest {
        val store = FakeConfigStore(AiCoachConfig(apiKey = ""))
        val provider = AiCoachConfigProvider(store)
        val machine = AiCoachStateMachine(RecordingCoach(), provider).launchIn(backgroundScope)

        machine.state.first { it is AiCoachUiState.ConfigMissing }

        // Simulates saving from the settings page (same store the provider observes).
        store.save(AiCoachConfig(apiKey = "new-key"))

        machine.state.first { it is AiCoachUiState.Idle }
    }

    @Test
    fun `Idle goes back to ConfigMissing when the key is removed`() = runTest {
        val store = FakeConfigStore(AiCoachConfig(apiKey = "test-key"))
        val provider = AiCoachConfigProvider(store)
        provider.config.first { it.configured }
        val machine = AiCoachStateMachine(RecordingCoach(), provider).launchIn(backgroundScope)

        machine.state.first { it is AiCoachUiState.Idle }

        store.save(AiCoachConfig(apiKey = ""))

        machine.state.first { it is AiCoachUiState.ConfigMissing }
    }

    private class FakeConfigStore(initial: AiCoachConfig) : IAiCoachConfigStore {
        private val state = MutableStateFlow(initial)
        override fun observe(): Flow<AiCoachConfig> = state
        override suspend fun save(config: AiCoachConfig) {
            state.value = config
        }
    }

    private class RecordingCoach : IAiCoach {
        val histories = mutableListOf<List<CoachMessage>>()
        override suspend fun recommendToday(history: List<CoachMessage>): AiCoachResult {
            histories += history
            return AiCoachResult.TodayPlan(
                sessionsUsed = 0,
                rounds = 1,
                parts = emptyList(),
                ignoredNames = emptyList(),
                assistantMessage = CoachMessage(
                    fromUser = false,
                    content = CoachMessageContent.PlanSummary(emptyList()),
                ),
            )
        }
    }
}
