package site.xiaozk.dailyfitness.aicoach.ui

import com.freeletics.flowredux2.FlowReduxStateMachine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.engine.AiCoachResult
import site.xiaozk.dailyfitness.aicoach.engine.CoachFailure
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessageContent
import site.xiaozk.dailyfitness.aicoach.engine.IAiCoach
import site.xiaozk.dailyfitness.repository.IAiCoachConfigStore
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig

/**
 * The conversation is the state: pending/failed turns are ordinary messages, so
 * "loading" is derived from the last message and the machine runs/cancels its
 * request through a `condition` on that predicate.
 *
 * These tests cover the message layout, the request/abort lifecycle and the
 * invariant that UI-only turns never reach the engine.
 */
class AiCoachStateMachineTest {

    @Test
    fun `refresh appends a local user bubble and a loading assistant bubble`() = runTest {
        val machine = machineWith(FakeConfigStore(AiCoachConfig(apiKey = "test-key")), HoldingCoach())

        val userContent = CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1))
        machine.dispatchAction(AiCoachUiAction.Refresh(userContent))
        val loading = machine.awaitReady { it.isLoading }

        assertEquals(2, loading.history.size)
        val pendingUser = loading.history[0]
        assertTrue(pendingUser.fromUser)
        assertEquals(userContent, pendingUser.content)
        assertEquals(0, pendingUser.turnId)
        val pendingAssistant = loading.history[1]
        assertFalse(pendingAssistant.fromUser)
        assertTrue(pendingAssistant.isLoading)
        assertEquals(0, pendingAssistant.turnId)
    }

    @Test
    fun `the real reply reuses the pending bubble id`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val coach = object : IAiCoach {
            override suspend fun recommendToday(history: List<CoachMessage>): AiCoachResult {
                gate.await()
                return planResult()
            }
        }
        val machine = machineWith(FakeConfigStore(AiCoachConfig(apiKey = "test-key")), coach)

        machine.dispatchAction(AiCoachUiAction.Refresh(CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1))))
        val loading = machine.awaitReady { it.isLoading }
        val pendingTurnId = loading.history.last().turnId

        gate.complete(Unit)
        val ready = machine.awaitReady { !it.isLoading && it.history.size == 2 }

        val reply = ready.history.last()
        assertFalse(reply.isLoading)
        // Same list key => LazyColumn reuses the item and the loading bubble morphs in place.
        assertEquals(pendingTurnId, reply.turnId)
    }

    @Test
    fun `cancel turns the pending turn into a cancelled failure message`() = runTest {
        val started = CompletableDeferred<Unit>()
        val coach = object : IAiCoach {
            override suspend fun recommendToday(history: List<CoachMessage>): AiCoachResult {
                started.complete(Unit)
                awaitCancellation()
            }
        }
        val machine = machineWith(FakeConfigStore(AiCoachConfig(apiKey = "test-key")), coach)

        val userContent = CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1))
        machine.dispatchAction(AiCoachUiAction.Refresh(userContent))
        started.await()
        machine.awaitReady { it.isLoading }

        machine.dispatchAction(AiCoachUiAction.Cancel)

        val ready = machine.awaitReady { !it.isLoading }
        // The user turn stays visible; the pending assistant bubble becomes the failure.
        assertEquals(2, ready.history.size)
        assertTrue(ready.history[0].fromUser)
        assertEquals(userContent, ready.history[0].content)
        val failure = ready.history[1].content
        assertTrue(failure is CoachMessageContent.Failure)
        assertEquals(CoachFailure.Cancelled, (failure as CoachMessageContent.Failure).failure)
        // An aborted turn never joins the model conversation.
        assertTrue(ready.requestHistory.isEmpty())
    }

    @Test
    fun `a failed request stays in the conversation but is not sent again`() = runTest {
        val coach = FailThenRecordCoach()
        val machine = machineWith(FakeConfigStore(AiCoachConfig(apiKey = "test-key")), coach)

        machine.dispatchAction(AiCoachUiAction.Refresh(CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1))))
        val failed = machine.awaitReady { !it.isLoading && coach.histories.size == 1 }
        assertTrue(failed.history.last().content is CoachMessageContent.Failure)

        machine.dispatchAction(AiCoachUiAction.Refresh(CoachMessageContent.PlanRequest(LocalDate(2025, 1, 2))))
        val ready = machine.awaitReady { !it.isLoading && coach.histories.size == 2 }

        // First request: empty history. Second: the failed turn never entered the
        // model conversation, so nothing UI-only leaks into the prompt.
        assertEquals(listOf(0, 0), coach.histories.map { it.size })
        // Rendered conversation keeps the failed turn plus the new successful one;
        // the model conversation commits only the successful pair.
        assertEquals(4, ready.history.size)
        assertTrue(ready.history[1].content is CoachMessageContent.Failure)
        assertEquals(2, ready.requestHistory.size)
    }

    @Test
    fun `no train parts is a page gate that can be retried from scratch`() = runTest {
        val machine = machineWith(
            FakeConfigStore(AiCoachConfig(apiKey = "test-key")),
            NoTrainPartsThenPlanCoach(),
        )

        machine.dispatchAction(AiCoachUiAction.Refresh(CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1))))
        val noParts = machine.state.first { it is AiCoachUiState.NoTrainParts } as AiCoachUiState.NoTrainParts
        // An empty library means no set could have been recorded today and no
        // conversation is worth keeping.
        assertEquals(0, noParts.setsToday)
        assertTrue(noParts.history.isEmpty())

        // Retry starts a fresh conversation once the library exists.
        machine.dispatchAction(AiCoachUiAction.Refresh(CoachMessageContent.PlanRequest(LocalDate(2025, 1, 2))))
        val ready = machine.awaitReady { !it.isLoading && it.history.size == 2 }
        assertTrue(ready.history.last().content is CoachMessageContent.PlanSummary)
    }

    @Test
    fun `keeps the full conversation across turns`() = runTest {
        val coach = RecordingCoach()
        val machine = machineWith(FakeConfigStore(AiCoachConfig(apiKey = "test-key")), coach)

        val turns = 7 // 14 messages, beyond the 5-round (10-message) request window.
        val userContent = CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1))
        repeat(turns) { index ->
            machine.dispatchAction(AiCoachUiAction.Refresh(userContent))
            machine.awaitReady { !it.isLoading && coach.histories.size == index + 1 }
        }

        val ready = machine.state.value as AiCoachUiState.Ready
        // UI state keeps everything...
        assertEquals(turns * 2, ready.history.size)
        assertEquals(turns * 2, ready.requestHistory.size)
        // ...and each request received the full committed history so far
        // (0, 2, 4, ... 12), proving the machine no longer truncates before the engine.
        assertEquals((0 until turns).map { it * 2 }, coach.histories.map { it.size })
        assertTrue(coach.histories.last().size > 10)
    }

    @Test
    fun `ConfigMissing moves to Ready once the settings page stores a key`() = runTest {
        val store = FakeConfigStore(AiCoachConfig(apiKey = ""))
        val machine = machineWith(store, RecordingCoach())

        machine.state.first { it is AiCoachUiState.ConfigMissing }

        // Simulates saving from the settings page (same store the provider observes).
        store.save(AiCoachConfig(apiKey = "new-key"))

        machine.state.first { it is AiCoachUiState.Ready }
    }

    @Test
    fun `Ready goes back to ConfigMissing when the key is removed`() = runTest {
        val store = FakeConfigStore(AiCoachConfig(apiKey = "test-key"))
        val machine = machineWith(store, RecordingCoach())

        machine.state.first { it is AiCoachUiState.Ready }

        store.save(AiCoachConfig(apiKey = ""))

        machine.state.first { it is AiCoachUiState.ConfigMissing }
    }

    @Test
    fun `startTurn is display-only and commitReply is the only model commit`() {
        val loading = AiCoachUiState.Ready(setsToday = 0)
            .startTurn(CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1)))

        // Pending: rendered, but not part of the model conversation yet.
        assertEquals(2, loading.history.size)
        assertTrue(loading.isLoading)
        assertTrue(loading.requestHistory.isEmpty())
        // Both sides of the turn share one turn id (the LazyColumn key source).
        assertNotNull(loading.history[0].turnId)
        assertEquals(loading.history[0].turnId, loading.history[1].turnId)

        // Failure/abort commits to the rendered list only.
        val failed = loading.commitFailure(CoachFailure.Timeout)
        assertEquals(2, failed.history.size)
        assertTrue(failed.history.last().content is CoachMessageContent.Failure)
        assertEquals(loading.history.last().turnId, failed.history.last().turnId)
        assertTrue(failed.requestHistory.isEmpty())

        // Success commits the pair to both lists.
        val replied = loading.commitReply(planResult().assistantMessage)
        assertEquals(2, replied.history.size)
        assertFalse(replied.isLoading)
        assertEquals(loading.history.last().turnId, replied.history.last().turnId)
        assertEquals(replied.history, replied.requestHistory)
    }
}

private typealias UiMachine = FlowReduxStateMachine<StateFlow<AiCoachUiState>, AiCoachUiAction>

private suspend fun TestScope.machineWith(
    store: IAiCoachConfigStore,
    coach: IAiCoach,
): UiMachine {
    val provider = AiCoachConfigProvider(store)
    // Let the eager provider pick up the initial store value before the machine starts,
    // so the first config emission is not the transient default.
    val expected = store.observe().first()
    provider.config.first { it == expected }
    val machine = AiCoachStateMachine(coach, provider).launchIn(backgroundScope)
    machine.state.first { it is AiCoachUiState.Ready || it is AiCoachUiState.ConfigMissing }
    return machine
}

private suspend fun UiMachine.awaitReady(predicate: (AiCoachUiState.Ready) -> Boolean): AiCoachUiState.Ready =
    state.first { it is AiCoachUiState.Ready && predicate(it) } as AiCoachUiState.Ready

private fun planResult(): AiCoachResult.TodayPlan = AiCoachResult.TodayPlan(
    sessionsUsed = 0,
    rounds = 1,
    parts = emptyList(),
    ignoredNames = emptyList(),
    assistantMessage = CoachMessage(
        fromUser = false,
        content = CoachMessageContent.PlanSummary(emptyList()),
    ),
)

private class FakeConfigStore(initial: AiCoachConfig) : IAiCoachConfigStore {
    private val state = MutableStateFlow(initial)
    override fun observe(): Flow<AiCoachConfig> = state
    override suspend fun save(config: AiCoachConfig) {
        state.value = config
    }
}

private class HoldingCoach : IAiCoach {
    override suspend fun recommendToday(history: List<CoachMessage>): AiCoachResult =
        awaitCancellation()
}

private class RecordingCoach : IAiCoach {
    val histories = mutableListOf<List<CoachMessage>>()
    override suspend fun recommendToday(history: List<CoachMessage>): AiCoachResult {
        histories += history
        return planResult()
    }
}

private class FailThenRecordCoach : IAiCoach {
    val histories = mutableListOf<List<CoachMessage>>()
    override suspend fun recommendToday(history: List<CoachMessage>): AiCoachResult {
        histories += history
        return if (histories.size == 1) {
            AiCoachResult.Failed(CoachFailure.ModelError("boom"), retryable = true)
        } else {
            planResult()
        }
    }
}

private class NoTrainPartsThenPlanCoach : IAiCoach {
    private var calls = 0
    override suspend fun recommendToday(history: List<CoachMessage>): AiCoachResult {
        calls++
        return if (calls == 1) AiCoachResult.NoTrainParts else planResult()
    }
}
