package site.xiaozk.dailyfitness.page.training.add

import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.engine.CoachSuggestion
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutMap
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.HomeWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import site.xiaozk.dailyfitness.repository.model.User
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit

/**
 * M3.3-0: FlowRedux state machine replacing the old `DailyWorkoutReducer`.
 * Covers the prefill-vs-last-workout routing and the submit path.
 */
class AddWorkoutStateMachineTest {

    @Test
    fun `suggestion prefills the form without reading the last workout`() = runTest {
        val parts = sampleParts()
        val workoutRepo = FakeWorkoutRepository(lastWorkout = lastWorkout())
        val machine = AddWorkoutStateMachine(
            workoutRepo,
            FakeTrainRepository(parts),
            FakeUserRepository(),
            CoachSuggestion(partName = "胸部", actionName = "卧推", sets = 4, reps = 8, weightKg = 60.0),
        ).launchIn(backgroundScope)

        val state = machine.state.first { it.allParts.isNotEmpty() }

        assertEquals("胸部", state.selectedPart?.part?.partName)
        assertEquals("卧推", state.selectedAction?.action?.actionName)
        assertEquals("8", state.count)
        assertEquals("60", state.weight)
        assertEquals(0, workoutRepo.lastWorkoutQueries)
    }

    @Test
    fun `without suggestion falls back to the last recorded action`() = runTest {
        val parts = sampleParts()
        val workoutRepo = FakeWorkoutRepository(lastWorkout = lastWorkout())
        val machine = AddWorkoutStateMachine(
            workoutRepo,
            FakeTrainRepository(parts),
            FakeUserRepository(),
            null,
        ).launchIn(backgroundScope)

        val state = machine.state.first { it.selectedAction != null }

        assertEquals("卧推", state.selectedAction?.action?.actionName)
        assertEquals("10", state.count)
        assertEquals("50", state.weight)
        assertEquals(WeightUnit.Kg, state.weightUnit)
        assertEquals(1, workoutRepo.lastWorkoutQueries)
    }

    @Test
    fun `select part clears the action and opens the action menu`() = runTest {
        val parts = sampleParts()
        val machine = AddWorkoutStateMachine(
            FakeWorkoutRepository(),
            FakeTrainRepository(parts),
            FakeUserRepository(),
            null,
        ).launchIn(backgroundScope)
        machine.state.first { it.allParts.isNotEmpty() }

        machine.dispatchAction(AddWorkoutAction.SelectPart(parts.first(), openActionSelection = true))
        val state = machine.state.first { it.selectedPart != null }

        assertEquals(null, state.selectedAction)
        assertTrue(state.showActionMenuState)
    }

    @Test
    fun `submit persists the current input and reports Done`() = runTest {
        val parts = sampleParts()
        val workoutRepo = FakeWorkoutRepository()
        val machine = AddWorkoutStateMachine(
            workoutRepo,
            FakeTrainRepository(parts),
            FakeUserRepository(),
            CoachSuggestion(partName = "胸部", actionName = "卧推", reps = 8, weightKg = 60.0),
        ).launchIn(backgroundScope)
        machine.state.first { it.selectedAction != null }

        machine.dispatchAction(AddWorkoutAction.Submit)
        val state = machine.state.first { it.submitStatus == ActionStatus.Done }

        assertEquals(ActionStatus.Done, state.submitStatus)
        val added = workoutRepo.added.single()
        assertEquals("卧推", added.action.actionName)
        assertEquals(8, added.takenCount)
        assertEquals(60f, added.takenWeight?.weight)
    }

    // ---------------------------------------------------------------- fixtures

    private fun sampleParts(): List<TrainPartGroup> {
        val chest = TrainPart(id = 1, partName = "胸部")
        val bench = TrainActionWithPart(
            part = chest,
            action = TrainAction(
                id = 11,
                partId = 1,
                actionName = "卧推",
                isWeightedAction = true,
                isCountedAction = true,
            ),
        )
        return listOf(TrainPartGroup(part = chest, actions = listOf(bench)))
    }

    private fun lastWorkout(): DailyWorkoutAction {
        return DailyWorkoutAction(
            instant = Clock.System.now(),
            action = TrainAction(
                id = 11,
                partId = 1,
                actionName = "卧推",
                isWeightedAction = true,
                isCountedAction = true,
            ),
            takenDuration = null,
            takenWeight = RecordedWeight(50f, WeightUnit.Kg),
            takenCount = 10,
            note = "last",
        )
    }

    private class FakeUserRepository : IUserRepository {
        override suspend fun getCurrentUser(): User = User(uid = 1, name = "tester")
        override suspend fun createUser(user: User) = error("unused")
    }

    private class FakeTrainRepository(
        private val parts: List<TrainPartGroup>,
    ) : ITrainActionRepository {
        override fun getAllTrainPartStatics(): Flow<HomeTrainPartPage> = error("unused")
        override fun getTrainPartStatic(partId: Int): Flow<TrainPartStaticPage?> = error("unused")
        override fun getTrainActionStatic(actionId: Int): Flow<TrainActionStaticPage?> = error("unused")
        override fun getAllTrainParts(): Flow<List<TrainPartGroup>> = flowOf(parts)
        override fun getActionsOfPart(partId: Int): Flow<TrainPartGroup> = error("unused")
        override suspend fun addTrainPart(part: TrainPart) = error("unused")
        override suspend fun updateTrainPart(part: TrainPart) = error("unused")
        override suspend fun removeTrainPart(part: TrainPart) = error("unused")
        override fun getAction(actionId: Int): Flow<TrainAction> = error("unused")
        override suspend fun addTrainAction(action: TrainAction) = error("unused")
        override suspend fun updateTrainAction(action: TrainAction) = error("unused")
        override suspend fun removeTrainAction(action: TrainAction) = error("unused")
    }

    private class FakeWorkoutRepository(
        private val lastWorkout: DailyWorkoutAction? = null,
    ) : IDailyWorkoutRepository {
        var lastWorkoutQueries = 0
        val added = mutableListOf<DailyWorkoutAction>()

        override fun getMonthWorkoutStatic(user: User, month: YearMonth): Flow<MonthWorkoutStatic> =
            error("unused")

        override fun getHomeWorkoutStatics(user: User, month: YearMonth): Flow<HomeWorkoutStatic> =
            error("unused")

        override fun getWorkoutDayList(
            user: User,
            from: LocalDate,
            to: LocalDate,
        ): Flow<DailyWorkoutMap> = error("unused")

        override fun getAllWorkoutDayList(user: User): Flow<DailyWorkoutMap> = error("unused")

        override suspend fun getWorkout(user: User, workoutId: Int): DailyWorkoutAction =
            error("unused")

        override suspend fun addWorkoutAction(user: User, action: DailyWorkoutAction) {
            added += action
        }

        override suspend fun deleteWorkoutAction(user: User, action: DailyWorkoutAction) =
            error("unused")

        override suspend fun getLastWorkout(
            user: User,
            date: LocalDate,
            zoneId: TimeZone,
        ): DailyWorkoutAction? {
            lastWorkoutQueries++
            return lastWorkout
        }
    }
}
