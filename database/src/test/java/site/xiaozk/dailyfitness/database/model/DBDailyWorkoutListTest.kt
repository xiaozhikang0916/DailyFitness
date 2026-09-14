package site.xiaozk.dailyfitness.database.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit
import kotlin.time.Instant

/**
 * Unit tests for [toDailyWorkoutList]: given rows already ordered oldest-first (the DAO's
 * `ORDER BY actionTime ASC`), the folded days/actions/sets must stay chronological while
 * still grouping several sets of the same action together.
 */
class DBDailyWorkoutListTest {

    private val zone = TimeZone.of("UTC")

    @Test
    fun `empty rows produce an empty list`() {
        assertEquals(emptyList<DailyWorkout>(), emptyList<DBDailyWorkoutRecord>().toDailyWorkoutList(zone))
    }

    @Test
    fun `keeps oldest-first order and merges sets of the same action`() {
        val day = LocalDate(2025, 1, 10)
        // Rows arrive exactly as `ORDER BY actionTime ASC` returns them.
        val rows = listOf(
            record(day, hour = 9, seq = 1, actionId = BENCH, action = "卧推", partId = CHEST, part = "胸部", weightKg = 60f, reps = 8),
            record(day, hour = 10, seq = 2, actionId = PULL_UP, action = "引体向上", partId = BACK, part = "背部", reps = 10),
            record(day, hour = 11, seq = 3, actionId = BENCH, action = "卧推", partId = CHEST, part = "胸部", weightKg = 65f, reps = 6),
        )

        val workout = rows.toDailyWorkoutList(zone).single()

        // Part/action groups keep first-encounter order: chest before back.
        assertEquals(listOf("胸部", "背部"), workout.actions.map { it.action.part.partName })
        val bench = workout.actions.first()
        assertEquals("卧推", bench.action.actionName)
        assertEquals(CHEST, bench.action.id)
        // Both 卧推 sets are merged and stay oldest-first.
        assertEquals(listOf(60f, 65f), bench.trainAction.map { it.takenWeight!!.weight })
        assertEquals(listOf(8, 6), bench.trainAction.map { it.takenCount })
    }

    @Test
    fun `splits rows into days ordered oldest-first`() {
        val older = LocalDate(2025, 1, 8)
        val newer = LocalDate(2025, 1, 10)
        val rows = listOf(
            record(older, hour = 9, seq = 1, actionId = BENCH, action = "卧推", partId = CHEST, part = "胸部", weightKg = 60f, reps = 8),
            record(newer, hour = 9, seq = 2, actionId = BENCH, action = "卧推", partId = CHEST, part = "胸部", weightKg = 62.5f, reps = 8),
        )

        val workouts = rows.toDailyWorkoutList(zone)

        assertEquals(listOf(older, newer), workouts.map { it.date })
        assertEquals(60f, workouts.first().actions.single().trainAction.single().takenWeight!!.weight)
        assertEquals(62.5f, workouts.last().actions.single().trainAction.single().takenWeight!!.weight)
    }

    @Test
    fun `converts recorded units and keeps per-action type flags`() {
        val day = LocalDate(2025, 1, 10)
        val rows = listOf(
            DBDailyWorkoutRecord(
                record = DBDailyWorkoutAction(
                    actionId = 1,
                    usingActionId = BENCH,
                    userId = USER,
                    actionTime = at(day, hour = 9),
                    recordedDuration = DBRecordedDuration(2f, DBTimeUnit.Min),
                    recordedWeight = DBRecordedWeight(100f, DBWeightUnit.Lbs),
                    takenCount = 10,
                    note = null,
                ),
                action = DBTrainAction(
                    id = BENCH,
                    actionName = "卧推",
                    partId = CHEST,
                    isTimedAction = true,
                    isWeightedAction = true,
                    isCountedAction = true,
                ),
                partName = "胸部",
            ),
        )

        val workout = rows.toDailyWorkoutList(zone).single()
        val action = workout.actions.single()
        assertTrue(action.action.isWeightedAction)
        assertTrue(action.action.isCountedAction)
        assertTrue(action.action.isTimedAction)
        val set = action.trainAction.single()
        assertEquals(120f, set.takenDuration!!.uniform().duration)
        assertEquals(100f * WeightUnit.Lbs.trans, set.takenWeight!!.uniform().weight, 0.01f)
        assertEquals(10, set.takenCount)
    }

    private fun record(
        date: LocalDate,
        hour: Int,
        seq: Int,
        actionId: Int,
        action: String,
        partId: Int,
        part: String,
        weightKg: Float? = null,
        reps: Int? = null,
    ) = DBDailyWorkoutRecord(
        record = DBDailyWorkoutAction(
            actionId = seq,
            usingActionId = actionId,
            userId = USER,
            actionTime = at(date, hour),
            recordedDuration = null,
            recordedWeight = weightKg?.let { DBRecordedWeight(it, DBWeightUnit.Kg) },
            takenCount = reps,
            note = null,
        ),
        action = DBTrainAction(
            id = actionId,
            actionName = action,
            partId = partId,
            isTimedAction = false,
            isWeightedAction = weightKg != null,
            isCountedAction = reps != null,
        ),
        partName = part,
    )

    private fun at(date: LocalDate, hour: Int): Instant =
        Instant.fromEpochMilliseconds(date.toEpochDays() * 86_400_000L + hour * 3_600_000L)

    private companion object {
        const val USER = 1
        const val CHEST = 1
        const val BACK = 2
        const val BENCH = 1
        const val PULL_UP = 2
    }
}
