package site.xiaozk.dailyfitness.aicoach.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.engine.AiCoachResult
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
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
    fun `keeps the full conversation across turns`() = runTest {
        val store = FakeConfigStore(AiCoachConfig(apiKey = "test-key"))
        val provider = AiCoachConfigProvider(store)
        provider.config.first { it.configured }

        val coach = RecordingCoach()
        val machine = AiCoachStateMachine(coach, provider).launchIn(backgroundScope)

        // Initial config probe -> Idle.
        machine.state.first { it is AiCoachUiState.Idle }

        val turns = 7 // 14 messages, beyond the 5-round (10-message) request window.
        repeat(turns) { index ->
            machine.dispatchAction(AiCoachUiAction.Refresh)
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
                newMessages = listOf(
                    CoachMessage(fromUser = true, text = "req ${histories.size}"),
                    CoachMessage(fromUser = false, text = "plan ${histories.size}"),
                ),
            )
        }
    }
}
