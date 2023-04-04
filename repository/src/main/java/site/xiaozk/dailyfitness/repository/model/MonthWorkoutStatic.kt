package site.xiaozk.dailyfitness.repository.model

import site.xiaozk.calendar.date.Month
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
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

    fun getWeekTrainedDay(today: LocalDate = LocalDate.now(), firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY): Int {
        val from = today.with(firstDayOfWeek).let {
            if (it.isAfter(today).not()) {
                it
            } else {
                it.minusDays(7)
            }
        }.toEpochDay()
        val to = from + 6
        val period = from..to
        return workoutDays.trainedDate.filter {
            it.key.toEpochDay() in period
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
    constructor(date: Instant, value: Float) : this(LocalDate.from(date.atZone(ZoneId.systemDefault())) to value)
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