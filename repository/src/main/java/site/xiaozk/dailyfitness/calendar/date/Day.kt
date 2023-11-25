package site.xiaozk.dailyfitness.calendar.date

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import site.xiaozk.dailyfitness.repository.model.YearMonth

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
        val diff = firstDayOfWeek.value - currentDayOfWeek.value
        val firstDay = this.date.plus(diff, DateTimeUnit.DAY).let {
            if (it <= this.date) {
                it
            } else {
                it.minus(7, DateTimeUnit.DAY)
            }
        }
        return (0..6).map {
            Day(firstDay.plus(it, DateTimeUnit.DAY))
        }.let(::ArrayList).let(::Week)
    }

    fun getMonth(): Month {
        val firstDay = LocalDate(this.date.year, this.date.month, 1)
        return (0 until this.date.dayOfMonth).map {
            Day(firstDay.plus(it, DateTimeUnit.DAY))
        }.let(::ArrayList).let(::Month)
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
        month.atDay(1).let {
            (0 until month.lengthOfMonth()).map { offset ->
                Day(it.plus(offset, DateTimeUnit.DAY))
            }.let(::ArrayList)
        }
    )

    override val yearMonth: YearMonth
        get() = YearMonth(days.first().date)

    fun getOverlappingMonth(firstDayOfWeek: DayOfWeek): OverlappingMonth {
        val firstDay = this.days.first().date
        val firstDayInWeekOfMonth = firstDay.dayOfWeek
        val diff = firstDayOfWeek.value - firstDayInWeekOfMonth.value
        val start = firstDay.plus(diff, DateTimeUnit.DAY).let {
            if (it <= firstDay) {
                it
            } else {
                it.minus(7, DateTimeUnit.DAY)
            }
        }
        val lastDay = this.days.last().date
        val diff2 = DayOfWeek.of((firstDayOfWeek.value + 6) % 7).value - lastDay.dayOfWeek.value
        val end = lastDay.plus(diff2, DateTimeUnit.DAY).let {
            if (it >= lastDay) {
                it
            } else {
                it.plus(7, DateTimeUnit.DAY)
            }
        }
        val prevDays = (0 until firstDay.toEpochDays() - start.toEpochDays()).map {
            Day(start.plus(it, DateTimeUnit.DAY))
        }.let(::ArrayList)
        val nextDays = (1..end.toEpochDays() - lastDay.toEpochDays()).map {
            Day(end.plus(it, DateTimeUnit.DAY))
        }.let(::ArrayList)
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