package site.xiaozk.dailyfitness.session

import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListActionPair
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutMap
import site.xiaozk.dailyfitness.repository.model.HomeWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.User

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionControllerTest {

    private class FakeSessionStore(initial: SessionMeta = SessionMeta()) : SessionStore {
        val meta = MutableStateFlow(initial)
        override fun observe(): Flow<SessionMeta> = meta
        override suspend fun setActive(meta: SessionMeta) {
            this.meta.value = meta
        }
    }

    private class FakeUserRepo : IUserRepository {
        override suspend fun getCurrentUser(): User = User()
        override suspend fun createUser(user: User) = Unit
    }

    private class FakeWorkoutRepo(
        private val workout: DailyWorkout? = null,
    ) : IDailyWorkoutRepository {
        override fun getWorkoutOfDayFlow(user: User, day: LocalDate): Flow<DailyWorkout?> =
            flowOf(workout)

        override fun getMonthWorkoutStatic(
            user: User,
            month: YearMonth,
        ): Flow<MonthWorkoutStatic> = flowOf(MonthWorkoutStatic(month))

        override fun getHomeWorkoutStatics(
            user: User,
            month: YearMonth,
        ): Flow<HomeWorkoutStatic> = flowOf(HomeWorkoutStatic(month))

        override fun getWorkoutDayList(
            user: User,
            from: LocalDate,
            to: LocalDate,
        ): Flow<DailyWorkoutMap> = flowOf(DailyWorkoutMap())

        override fun getAllWorkoutDayList(user: User): Flow<DailyWorkoutMap> =
            flowOf(DailyWorkoutMap())

        override suspend fun getWorkout(user: User, workoutId: Int): DailyWorkoutAction =
            error("not used")

        override suspend fun addWorkoutAction(user: User, action: DailyWorkoutAction) = Unit

        override suspend fun deleteWorkoutAction(user: User, action: DailyWorkoutAction) = Unit

        override suspend fun getLastWorkout(
            user: User,
            date: LocalDate,
            zoneId: TimeZone,
        ): DailyWorkoutAction? = null
    }

    @Test
    fun `start turns state active, finish turns it inactive`() = runTest {
        val controller = newController(this, FakeSessionStore())
        assertFalse(controller.state.value.active)

        controller.start()
        assertTrue(controller.state.value.active)
        assertNotNull(controller.state.value.startedAt)

        controller.finish()
        assertFalse(controller.state.value.active)
        assertTrue(controller.state.value.startedAt == null)
    }

    @Test
    fun `session is restored after simulated process death`() = runTest {
        // first "process": start the session
        val store = FakeSessionStore()
        newController(this, store).start()

        // second "process": a fresh controller reads the same store
        val restored = newController(this, store)
        assertTrue(restored.state.value.active)
        assertNotNull(restored.state.value.startedAt)
    }

    @Test
    fun `derived state reflects workout flow only when active`() = runTest {
        val store = FakeSessionStore()
        val controller = newController(this, store, workout = workoutOfOneSet())
        assertFalse(controller.state.value.active)
        assertTrue(controller.state.value.currentActionId == null)

        controller.start()
        assertTrue(controller.state.value.active)
        assertNotNull(controller.state.value.currentActionId)
        assertTrue(controller.state.value.setsDone > 0)
    }

    private fun newController(
        testScope: TestScope,
        store: SessionStore,
        workout: DailyWorkout? = null,
    ): WorkoutSessionController {
        val scope = CoroutineScope(
            SupervisorJob() + UnconfinedTestDispatcher(testScope.testScheduler)
        )
        return WorkoutSessionControllerImpl(
            store = store,
            workoutRepo = FakeWorkoutRepo(workout),
            userRepo = FakeUserRepo(),
        ).apply {
            this.scope = scope
        }
    }

    private fun workoutOfOneSet(): DailyWorkout {
        val action = TrainAction(id = 10, actionName = "push-up", isCountedAction = true)
        val set = DailyWorkoutAction(
            id = 1,
            instant = Instant.fromEpochMilliseconds(1_000L),
            action = action,
            takenDuration = null,
            takenWeight = null,
            takenCount = 1,
            note = "",
        )
        return DailyWorkout(
            date = LocalDate(2025, 1, 1),
            actions = listOf(
                DailyWorkoutListActionPair(
                    TrainActionWithPart(part = TrainPart(id = 1, partName = "chest"), action = action),
                    listOf(set),
                )
            ),
        )
    }
}
