package site.xiaozk.dailyfitness.aicoach.ui

import com.freeletics.flowredux2.FlowReduxStateMachine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
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
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.HomeWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.User

/**
 * The conversation is the state: pending/failed turns are ordinary messages, so
 * "loading" is derived from the last message and the machine runs/cancels its
 * request through a `condition` on that predicate.
 *
 * The machine also owns the observed today-training state and builds the user turn
 * itself, so the ViewModel stays stateless.
 */
class AiCoachStateMachineTest {

    @Test
    fun `refresh builds a plan request when nothing was trained today`() = runTest {
        val machine = machineWith(FakeConfigStore(AiCoachConfig(apiKey = "test-key")), HoldingCoach())

        machine.dispatchAction(AiCoachUiAction.Refresh)
        val loading = machine.awaitReady { it.isLoading }

        assertEquals(2, loading.history.size)
        val pendingUser = loading.history[0]
        assertTrue(pendingUser.fromUser)
        assertTrue(pendingUser.content is CoachMessageContent.PlanRequest)
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

        machine.dispatchAction(AiCoachUiAction.Refresh)
        val loading = machine.awaitReady { it.isLoading }
        val pendingTurnId = loading.history.last().turnId

        gate.complete(Unit)
        val ready = machine.awaitReady { !it.isLoading && it.history.size == 2 }

        val reply = ready.history.last()
        assertFalse(reply.isLoading)
        // Same turn id => LazyColumn reuses the item and the loading bubble morphs in place.
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

        machine.dispatchAction(AiCoachUiAction.Refresh)
        started.await()
        machine.awaitReady { it.isLoading }

        machine.dispatchAction(AiCoachUiAction.Cancel)

        val ready = machine.awaitReady { !it.isLoading }
        // The user turn stays visible; the pending assistant bubble becomes the failure.
        assertEquals(2, ready.history.size)
        assertTrue(ready.history[0].fromUser)
        assertTrue(ready.history[0].content is CoachMessageContent.PlanRequest)
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

        machine.dispatchAction(AiCoachUiAction.Refresh)
        val failed = machine.awaitReady { !it.isLoading && coach.histories.size == 1 }
        assertTrue(failed.history.last().content is CoachMessageContent.Failure)

        machine.dispatchAction(AiCoachUiAction.Refresh)
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
    fun `an agent authored surface is committed as an AgentUi turn`() = runTest {
        val messages = listOf(
            """{"version":"v0.9","createSurface":{"surfaceId":"plan-1"}}""",
            """{"version":"v0.9","updateComponents":{"surfaceId":"plan-1","components":[]}}""",
        )
        val coach = object : IAiCoach {
            override suspend fun recommendToday(history: List<CoachMessage>): AiCoachResult =
                AiCoachResult.AgentUi(
                    surfaceId = "plan-1",
                    messages = messages,
                    assistantMessage = CoachMessage(
                        fromUser = false,
                        content = CoachMessageContent.AgentUi("plan-1", messages),
                    ),
                )
        }
        val machine = machineWith(FakeConfigStore(AiCoachConfig(apiKey = "test-key")), coach)

        machine.dispatchAction(AiCoachUiAction.Refresh)
        val ready = machine.awaitReady { !it.isLoading && it.history.size == 2 }

        val reply = ready.history.last()
        val content = reply.content as? CoachMessageContent.AgentUi ?: error("expected AgentUi")
        assertEquals("plan-1", content.surfaceId)
        assertEquals(2, content.messages.size)
        // The reply replaces the pending bubble inside the same turn.
        assertEquals(ready.history.first().turnId, reply.turnId)
        // The agent turn joins the model conversation like any other reply.
        assertEquals(2, ready.requestHistory.size)
    }

    @Test
    fun `no train parts is a page gate that can be retried from scratch`() = runTest {
        val machine = machineWith(
            FakeConfigStore(AiCoachConfig(apiKey = "test-key")),
            NoTrainPartsThenPlanCoach(),
        )

        machine.dispatchAction(AiCoachUiAction.Refresh)
        val noParts = machine.state.first { it is AiCoachUiState.NoTrainParts } as AiCoachUiState.NoTrainParts
        // An empty library means no set could have been recorded today and no
        // conversation is worth keeping.
        assertEquals(0, noParts.setsToday)
        assertTrue(noParts.history.isEmpty())

        // Retry starts a fresh conversation once the library exists.
        machine.dispatchAction(AiCoachUiAction.Refresh)
        val ready = machine.awaitReady { !it.isLoading && it.history.size == 2 }
        assertTrue(ready.history.last().content is CoachMessageContent.PlanSummary)
    }

    @Test
    fun `keeps the full conversation across turns`() = runTest {
        val coach = RecordingCoach()
        val machine = machineWith(FakeConfigStore(AiCoachConfig(apiKey = "test-key")), coach)

        val turns = 7 // 14 messages, beyond the 5-round (10-message) request window.
        repeat(turns) { index ->
            machine.dispatchAction(AiCoachUiAction.Refresh)
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

    @Test
    fun `reconciledWith gates on config and seeds today's training`() {
        val today = TodayTraining(
            setsToday = 3,
            currentPart = "胸部",
            trainedParts = listOf("胸部", "背部"),
            currentAction = "卧推",
            currentActionSets = 2,
        )
        val configured = Observed(configured = true, today = today)
        val unconfigured = Observed(configured = false, today = today)

        // Initial / ConfigMissing become Ready already carrying today's data.
        assertEquals(
            AiCoachUiState.Ready(
                setsToday = 3,
                currentPart = "胸部",
                trainedParts = listOf("胸部", "背部"),
                currentAction = "卧推",
                currentActionSets = 2,
            ),
            AiCoachUiState.Initial.reconciledWith(configured),
        )
        assertEquals(
            AiCoachUiState.Ready(
                setsToday = 3,
                currentPart = "胸部",
                trainedParts = listOf("胸部", "背部"),
                currentAction = "卧推",
                currentActionSets = 2,
            ),
            AiCoachUiState.ConfigMissing.reconciledWith(configured),
        )

        // Ready refreshes today's data but keeps both conversation lists.
        val ready = AiCoachUiState.Ready(setsToday = 0)
            .startTurn(CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1)))
        val reconciled = ready.reconciledWith(configured) as AiCoachUiState.Ready
        assertEquals(3, reconciled.setsToday)
        assertEquals("胸部", reconciled.currentPart)
        assertEquals(listOf("胸部", "背部"), reconciled.trainedParts)
        assertEquals("卧推", reconciled.currentAction)
        assertEquals(2, reconciled.currentActionSets)
        assertEquals(ready.history, reconciled.history)
        assertEquals(ready.requestHistory, reconciled.requestHistory)

        // NoTrainParts ignores today: an empty library means zero sets.
        assertEquals(AiCoachUiState.NoTrainParts, AiCoachUiState.NoTrainParts.reconciledWith(configured))

        // A missing key always gates any settled state back to ConfigMissing.
        assertEquals(AiCoachUiState.ConfigMissing, AiCoachUiState.Ready(setsToday = 3).reconciledWith(unconfigured))
        assertEquals(AiCoachUiState.ConfigMissing, AiCoachUiState.NoTrainParts.reconciledWith(unconfigured))
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
    val machine = AiCoachStateMachine(
        aiCoach = coach,
        configProvider = provider,
        userRepository = FakeUserRepository(),
        workoutRepository = FakeWorkoutRepository(),
    ).launchIn(backgroundScope)
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

private class FakeUserRepository : IUserRepository {
    override suspend fun getCurrentUser(): User = User(uid = 1, name = "test")
    override suspend fun createUser(user: User) = Unit
}

/** No workouts today, so the machine builds a PlanRequest turn. */
private class FakeWorkoutRepository : IDailyWorkoutRepository {
    override fun getWorkoutDayList(
        user: User,
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<DailyWorkout>> = flowOf(emptyList())

    override fun getAllWorkoutDayList(user: User): Flow<List<DailyWorkout>> = flowOf(emptyList())

    override fun getMonthWorkoutStatic(user: User, month: YearMonth): Flow<MonthWorkoutStatic> = TODO()
    override fun getHomeWorkoutStatics(user: User, month: YearMonth): Flow<HomeWorkoutStatic> = TODO()
    override suspend fun getWorkout(user: User, workoutId: Int): DailyWorkoutAction = TODO()
    override suspend fun addWorkoutAction(user: User, action: DailyWorkoutAction) = TODO()
    override suspend fun deleteWorkoutAction(user: User, action: DailyWorkoutAction) = TODO()
    override suspend fun getLastWorkout(user: User, date: LocalDate, zoneId: TimeZone): DailyWorkoutAction? = TODO()
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
