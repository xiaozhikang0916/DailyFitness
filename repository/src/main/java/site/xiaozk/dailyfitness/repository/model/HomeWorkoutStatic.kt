package site.xiaozk.dailyfitness.repository.model

import site.xiaozk.calendar.date.Month
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * @author: xiaozhikang
 * @create: 2023/4/2
 */
data class HomeWorkoutStatic(
    val month: YearMonth,
    val workoutDays: WorkoutDayList = WorkoutDayList(emptyMap()),
    val weight: BodyStatic? = null,
    val bustSize: BodyStatic? = null,
    val waistSize: BodyStatic? = null,
    val hipSize: BodyStatic? = null,
    val bodyFat: BodyStatic? = null,
) {
    val monthTrainedDay: Int
        get() = workoutDays.trainedDate.size

    val displayMonth: Month = Month(month)

    operator fun get(date: LocalDate): DailyWorkout? = workoutDays[date]

    fun getWeekTrainedDay(today: LocalDate = LocalDate.now(), firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY): Int {
        val from = today.with(firstDayOfWeek).let {
            if (it.isAfter(today).not()) {
                it
            } else {
                it.minusDays(7)
            }
        }.toEpochDay()
        val to = from + 6
        val period = from .. to
        return workoutDays.trainedDate.filter {
            it.key.toEpochDay() in period
        }.size
    }
}

@JvmInline
value class BodyStatic(
    val data: Pair<LocalDate, Float>,
) {
    constructor(date: Instant, value: Float) : this(LocalDate.from(date.atZone(ZoneId.systemDefault())) to value)
}