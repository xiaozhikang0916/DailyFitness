package site.xiaozk.dailyfitness.repository.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import site.xiaozk.dailyfitness.calendar.date.Month
import java.time.DayOfWeek
import java.util.TreeMap

/**
 * @author: xiaozhikang
 * @create: 2023/4/2
 */
data class MonthWorkoutStatic(
    val month: YearMonth,
    val workoutDays: WorkoutDaySummaryMap = WorkoutDaySummaryMap(),
) {
    val monthTrainedDay: Int
        get() = workoutDays.trainedDate.size

    val displayMonth: Month = Month(month)

    operator fun get(date: LocalDate): DailyWorkoutSummary? = workoutDays[date]

    fun getWeekTrainedDay(
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY
    ): Int {
        val diff = firstDayOfWeek.value - today.dayOfWeek.value
        val from = today.plus(diff, DateTimeUnit.DAY).let {
            if (it <= today) {
                it
            } else {
                it.minus(7, DateTimeUnit.DAY)
            }
        }.toEpochDays()
        val to = from + 6
        val period = from..to
        return workoutDays.trainedDate.filter {
            it.key.toEpochDays() in period
        }.size
    }
}

data class HomeWorkoutStatic(
    val monthStatic: MonthWorkoutStatic,
    val weight: BodyStatic? = null,
    val bustSize: BodyStatic? = null,
    val waistSize: BodyStatic? = null,
    val hipSize: BodyStatic? = null,
    val bodyFat: BodyStatic? = null,
) {
    constructor(month: YearMonth) : this(MonthWorkoutStatic(month))
}

@JvmInline
value class BodyStatic(
    val data: Pair<LocalDate, Float>,
) {
    constructor(
        date: Instant,
        value: Float
    ) : this(date.toLocalDateTime(TimeZone.currentSystemDefault()).date to value)
}

data class DailyWorkoutSummary(
    val date: LocalDate,
    val partsGroup: Map<TrainPart, List<DailyWorkoutListActionPair>> = emptyMap(),
) {
    constructor(daily: DailyWorkout) : this(
        date = daily.date,
        partsGroup = daily.actions.groupBy { it.action.part }
    )
}

@JvmInline
value class WorkoutDaySummaryMap(
    val trainedDate: TreeMap<LocalDate, DailyWorkoutSummary> = TreeMap(),
) {
    operator fun get(date: LocalDate): DailyWorkoutSummary? = trainedDate[date]
}