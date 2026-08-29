package site.xiaozk.dailyfitness.calendar.date

import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth

/**
 * @author: xiaozhikang
 * @create: 2023/4/1
 */

interface IDay {
    val date: LocalDate
    val isToday: Boolean
}

data class Day(
    override val date: LocalDate,
) : IDay, Comparable<IDay> {
    companion object {
        val Today: Day
            get() = Day(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    }

    override val isToday: Boolean
        get() = this == Today

    fun getWeek(firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY): Week {
        val currentDayOfWeek = this.date.dayOfWeek
        val diff = firstDayOfWeek.isoDayNumber - currentDayOfWeek.isoDayNumber
        val firstDay = this.date.plus(diff, DateTimeUnit.DAY).let {
            if (it <= this.date) {
                it
            } else {
                it.minus(7, DateTimeUnit.DAY)
            }
        }
        return (0..6).map {
            Day(firstDay.plus(it, DateTimeUnit.DAY))
        }.let { ArrayList(it) }.let(::Week)
    }

    fun getMonth(): Month {
        val firstDay = LocalDate(this.date.year, this.date.month, 1)
        return (0 until this.date.day).map {
            Day(firstDay.plus(it, DateTimeUnit.DAY))
        }.let { ArrayList(it) }.let(::Month)
    }

    override fun compareTo(other: IDay): Int {
        return this.date.compareTo(other.date)
    }
}

interface IDaysCollection<D : IDay> {
    val days: ArrayList<D>

    /**
     * check if a day is belonging to this collection.
     * E.g. a day in [OverlappingMonth.prevDays] is NOT in this collection
     * @return
     */
    fun IDay.inCurrentRange(): Boolean = true
}

interface IWeek<D : IDay> : IDaysCollection<D>

data class Week(
    override val days: ArrayList<Day>,
) : IWeek<Day>

data class WeekOfMonth(
    val month: Month,
    override val days: ArrayList<Day>,
) : IWeek<Day> {
    override fun IDay.inCurrentRange(): Boolean {
        return with(month) {
            this@inCurrentRange.inCurrentRange()
        }
    }
}

interface IMonth<D : IDay, W : IWeek<out D>> : IDaysCollection<D> {
    val yearMonth: YearMonth
    fun getWeeks(firstDayOfWeek: DayOfWeek): List<W>
}

data class Month(
    override val days: ArrayList<Day>,
) : IMonth<Day, WeekOfMonth> {
    companion object {
        val CurrentMonth: Month
            get() = Day.Today.getMonth()
    }

    constructor(month: YearMonth) : this(
        month.firstDay.let {
            (0 until month.numberOfDays).map { offset ->
                Day(it.plus(offset, DateTimeUnit.DAY))
            }.let { ArrayList(it) }
        }
    )

    override val yearMonth: YearMonth
        get() = days.first().date.yearMonth

    fun getOverlappingMonth(firstDayOfWeek: DayOfWeek): OverlappingMonth {
        val firstDay = this.days.first().date
        val firstDayInWeekOfMonth = firstDay.dayOfWeek
        val diff = firstDayOfWeek.isoDayNumber - firstDayInWeekOfMonth.isoDayNumber
        val start = firstDay.plus(diff, DateTimeUnit.DAY).let {
            if (it <= firstDay) {
                it
            } else {
                it.minus(7, DateTimeUnit.DAY)
            }
        }
        val lastDay = this.days.last().date
        val diff2 = DayOfWeek((firstDayOfWeek.isoDayNumber + 6) % 7).isoDayNumber - lastDay.dayOfWeek.isoDayNumber
        val end = lastDay.plus(diff2, DateTimeUnit.DAY).let {
            if (it >= lastDay) {
                it
            } else {
                it.plus(7, DateTimeUnit.DAY)
            }
        }
        val prevDays = (0 until firstDay.toEpochDays() - start.toEpochDays()).map {
            Day(start.plus(it, DateTimeUnit.DAY))
        }.let { ArrayList(it) }
        val nextDays = (1..end.toEpochDays() - lastDay.toEpochDays()).map {
            Day(end.plus(it, DateTimeUnit.DAY))
        }.let { ArrayList(it) }
        return OverlappingMonth(this, prevDays, nextDays)
    }

    override fun getWeeks(firstDayOfWeek: DayOfWeek): List<WeekOfMonth> {
        return getOverlappingMonth(firstDayOfWeek).getWeeks(firstDayOfWeek)
    }

    override fun IDay.inCurrentRange(): Boolean {
        return this.date >= days.first().date && this.date <= days.last().date
    }
}

data class OverlappingMonth(
    val currentMonth: Month,
    val prevDays: ArrayList<Day>,
    val nextDays: ArrayList<Day>,
) : IMonth<Day, WeekOfMonth> by currentMonth {
    override val days: ArrayList<Day> = ArrayList(prevDays + currentMonth.days + nextDays)

    val firstDayOfWeek: DayOfWeek = days.first().date.dayOfWeek

    override fun getWeeks(firstDayOfWeek: DayOfWeek): List<WeekOfMonth> {
        return if (firstDayOfWeek == this.firstDayOfWeek) {
            days.chunked(7).map { WeekOfMonth(currentMonth, ArrayList(it)) }
        } else {
            currentMonth.getOverlappingMonth(firstDayOfWeek).getWeeks(firstDayOfWeek)
        }
    }

    override fun IDay.inCurrentRange(): Boolean {
        return with(currentMonth) {
            this@inCurrentRange.inCurrentRange()
        }
    }
}