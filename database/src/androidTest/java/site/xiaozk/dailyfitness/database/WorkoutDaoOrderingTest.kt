package site.xiaozk.dailyfitness.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import site.xiaozk.dailyfitness.database.dao.getAllDailyWorkoutActions
import site.xiaozk.dailyfitness.database.dao.getWorkoutDayList
import site.xiaozk.dailyfitness.database.model.DBDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.DBRecordedWeight
import site.xiaozk.dailyfitness.database.model.DBTrainAction
import site.xiaozk.dailyfitness.database.model.DBTrainPart
import site.xiaozk.dailyfitness.database.model.DBUser
import site.xiaozk.dailyfitness.database.model.DBWeightUnit
import site.xiaozk.dailyfitness.database.utils.getEndEpochMillis
import site.xiaozk.dailyfitness.database.utils.getStartEpochMillis
import kotlin.time.Instant

/**
 * Room in-memory database test for the ordered workout queries.
 *
 * Verifies the actual SQL (`ORDER BY actionTime ASC`) together with the
 * [DBDailyWorkoutRecord] mapping and the `Flow<List<DailyWorkout>>` extensions used by the
 * repository and the AI Coach.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutDaoOrderingTest {

    private lateinit var db: AppDataBase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDataBase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun recordsComeBackOldestFirstWithActionAndPartResolved() = runTest {
        seedTrainLibrary()
        // Insert deliberately out of order so the ORDER BY is what actually sorts them.
        addSet(day = DAY, hour = 11, actionId = BENCH, weightKg = 65f, reps = 6)
        addSet(day = DAY, hour = 9, actionId = BENCH, weightKg = 60f, reps = 8)
        addSet(day = DAY, hour = 10, actionId = PULL_UP, weightKg = null, reps = 10)

        val rows = db.dailyDao().getAllWorkoutRecords(USER).first()

        assertEquals(listOf(9, 10, 11), rows.map { it.record.actionTime.toLocalHour() })
        assertEquals(listOf("卧推", "引体向上", "卧推"), rows.map { it.action.actionName })
        assertEquals(listOf("胸部", "背部", "胸部"), rows.map { it.partName })
        assertEquals(60f, rows.first().record.recordedWeight!!.takenWeight)
    }

    @Test
    fun allWorkoutsExtensionFoldsIntoChronologicalDailyWorkouts() = runTest {
        seedTrainLibrary()
        addSet(day = DAY, hour = 11, actionId = BENCH, weightKg = 65f, reps = 6)
        addSet(day = DAY, hour = 9, actionId = BENCH, weightKg = 60f, reps = 8)
        addSet(day = DAY, hour = 10, actionId = PULL_UP, weightKg = null, reps = 10)

        val workouts = db.dailyDao().getAllDailyWorkoutActions(USER).first()

        val workout = workouts.single()
        assertEquals(DAY, workout.date)
        // Chest was trained first (09:00), so it is the first part.
        assertEquals(listOf("胸部", "背部"), workout.actions.map { it.action.part.partName })
        val bench = workout.actions.first()
        // Both bench sets merged, oldest-first.
        assertEquals(listOf(60f, 65f), bench.trainAction.map { it.takenWeight!!.weight })
        assertEquals(listOf(8, 6), bench.trainAction.map { it.takenCount })
    }

    @Test
    fun dayListExtensionOnlyReturnsTheRequestedRangeOldestFirst() = runTest {
        seedTrainLibrary()
        val older = DAY.minusDays(2)
        addSet(day = older, hour = 9, actionId = BENCH, weightKg = 55f, reps = 8)
        addSet(day = DAY, hour = 9, actionId = BENCH, weightKg = 60f, reps = 8)

        val inRange = db.dailyDao()
            .getWorkoutDayList(USER, DAY.getStartEpochMillis(zone), DAY.getEndEpochMillis(zone))
            .first()
        assertEquals(listOf(DAY), inRange.map { it.date })

        val bothDays = db.dailyDao()
            .getWorkoutDayList(USER, older.getStartEpochMillis(zone), DAY.getEndEpochMillis(zone))
            .first()
        assertEquals(listOf(older, DAY), bothDays.map { it.date })
    }

    private suspend fun seedTrainLibrary() {
        db.userDao().createUser(DBUser(uid = USER, name = "test"))
        db.trainDao().addTrainPart(DBTrainPart(id = CHEST, partName = "胸部"))
        db.trainDao().addTrainPart(DBTrainPart(id = BACK, partName = "背部"))
        db.trainDao().addTrainAction(
            DBTrainAction(
                id = BENCH, actionName = "卧推", partId = CHEST,
                isTimedAction = false, isWeightedAction = true, isCountedAction = true,
            )
        )
        db.trainDao().addTrainAction(
            DBTrainAction(
                id = PULL_UP, actionName = "引体向上", partId = BACK,
                isTimedAction = false, isWeightedAction = false, isCountedAction = true,
            )
        )
    }

    private suspend fun addSet(day: LocalDate, hour: Int, actionId: Int, weightKg: Float?, reps: Int) {
        db.dailyDao().addDailyWorkoutAction(
            DBDailyWorkoutAction(
                usingActionId = actionId,
                userId = USER,
                actionTime = Instant.fromEpochMilliseconds(
                    day.getStartEpochMillis(zone) + hour * 3_600_000L
                ),
                recordedDuration = null,
                recordedWeight = weightKg?.let { DBRecordedWeight(it, DBWeightUnit.Kg) },
                takenCount = reps,
                note = null,
            )
        )
    }

    private fun Instant.toLocalHour(): Int =
        ((toEpochMilliseconds() % 86_400_000L) / 3_600_000L).toInt()

    private fun LocalDate.minusDays(days: Int): LocalDate =
        LocalDate.fromEpochDays(toEpochDays() - days)

    private companion object {
        const val USER = 1
        const val CHEST = 1
        const val BACK = 2
        const val BENCH = 1
        const val PULL_UP = 2
        val DAY = LocalDate(2025, 1, 10)
        val zone = TimeZone.of("UTC")
    }
}
