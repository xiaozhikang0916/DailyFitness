package site.xiaozk.dailyfitness.settings

import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readString
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IPersonDailyRepository
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.HomeWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import site.xiaozk.dailyfitness.repository.model.User

/**
 * Regression test for the export hang: [IPersonDailyRepository.getAllPersonDailyDataFlow]
 * is a Room flow that never completes (it keeps observing the table). Collecting it with
 * `toList()` blocks the export forever; the current snapshot must be taken with `first()`.
 */
class FitnessSettingsTest {

    @Test
    fun `export completes and writes json even though the body flow never completes`() =
        runTest(timeout = 10.seconds) {
            val directory = Path(
                SystemTemporaryDirectory,
                "dailyfitness-test-${Random.nextLong()}",
            )
            SystemFileSystem.createDirectories(directory)
            val file = Path(directory, "export.json")
            try {
                val settings = FitnessSettings(
                    userRepo = FakeUserRepository(),
                    dailyWorkoutRepo = FakeWorkoutRepository(),
                    personDataRepo = NeverEndingPersonRepository(),
                    trainRepo = FakeTrainActionRepository(),
                )

                settings.exportAllDataTo(file)

                val json = SystemFileSystem.source(file).buffered().use { it.readString() }
                assertTrue(json.contains("\"name\":\"test\""))
            } finally {
                SystemFileSystem.delete(file, mustExist = false)
                SystemFileSystem.delete(directory, mustExist = false)
            }
        }

    private class NeverEndingPersonRepository : IPersonDailyRepository {
        // StateFlow represents an always-observing Room flow: it emits a snapshot
        // but never completes.
        private val data = MutableStateFlow(BodyDataWithDate())

        override fun getAllPersonDailyDataFlow(user: User): Flow<BodyDataWithDate> = data

        override fun getPersonDailyDataFlow(
            user: User,
            from: LocalDate,
            to: LocalDate,
        ): Flow<BodyDataWithDate> = TODO()

        override suspend fun addPersonDailyData(user: User, data: BodyDataRecord) = TODO()

        override suspend fun updatePersonDailyData(user: User, data: BodyDataRecord) = TODO()

        override suspend fun removePersonDailyData(data: BodyDataRecord) = TODO()
    }

    private class FakeUserRepository : IUserRepository {
        override suspend fun getCurrentUser(): User = User(uid = 1, name = "test")

        override suspend fun createUser(user: User) = Unit
    }

    private class FakeWorkoutRepository : IDailyWorkoutRepository {
        override fun getAllWorkoutDayList(user: User): Flow<List<DailyWorkout>> =
            flowOf(emptyList())

        override fun getWorkoutDayList(
            user: User,
            from: LocalDate,
            to: LocalDate,
        ): Flow<List<DailyWorkout>> = TODO()

        override fun getMonthWorkoutStatic(user: User, month: YearMonth): Flow<MonthWorkoutStatic> =
            TODO()

        override fun getHomeWorkoutStatics(user: User, month: YearMonth): Flow<HomeWorkoutStatic> =
            TODO()

        override suspend fun getWorkout(user: User, workoutId: Int): DailyWorkoutAction = TODO()

        override suspend fun addWorkoutAction(user: User, action: DailyWorkoutAction) = TODO()

        override suspend fun deleteWorkoutAction(user: User, action: DailyWorkoutAction) = TODO()

        override suspend fun getLastWorkout(
            user: User,
            date: LocalDate,
            zoneId: TimeZone,
        ): DailyWorkoutAction? = TODO()
    }

    private class FakeTrainActionRepository : ITrainActionRepository {
        override fun getAllTrainParts(): Flow<List<TrainPartGroup>> = flowOf(emptyList())

        override fun getAllTrainPartStatics(): Flow<HomeTrainPartPage> = TODO()

        override fun getTrainPartStatic(partId: Int): Flow<TrainPartStaticPage?> = TODO()

        override fun getTrainActionStatic(actionId: Int): Flow<TrainActionStaticPage?> = TODO()

        override fun getActionsOfPart(partId: Int): Flow<TrainPartGroup> = TODO()

        override fun getAction(actionId: Int): Flow<TrainAction> = TODO()

        override suspend fun addTrainPart(part: TrainPart) = TODO()

        override suspend fun updateTrainPart(part: TrainPart) = TODO()

        override suspend fun removeTrainPart(part: TrainPart) = TODO()

        override suspend fun addTrainAction(action: TrainAction) = TODO()

        override suspend fun updateTrainAction(action: TrainAction) = TODO()

        override suspend fun removeTrainAction(action: TrainAction) = TODO()
    }
}
