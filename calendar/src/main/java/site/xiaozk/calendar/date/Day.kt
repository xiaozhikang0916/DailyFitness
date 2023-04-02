package site.xiaozk.calendar.date

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoField

/**
 * @author: xiaozhikang
 * @create: 2023/4/1
 */

interface IDay {
    val date: LocalDate
    val isToday: Boolean
    val isCurrentWeek: Boolean
    val isCurrentMonth: Boolean
}

data class Day(
    override val date: LocalDate,
) : IDay, Comparable<IDay> {
    companion object {
        val Today: Day
            get() = Day(LocalDate.now())
    }

    override val isToday: Boolean
        get() = date == LocalDate.now()
    override val isCurrentMonth: Boolean
        get() = LocalDate.now().let {
            it.month == this.date.month && it.year == this.date.year
        }
    override val isCurrentWeek: Boolean
        get() = LocalDate.now().let {
            it.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == this.date.get(ChronoField.ALIGNED_WEEK_OF_YEAR) && it.year == this.date.year
        }

    fun getWeek(firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY): Week {
        val firstDay = this.date.with(firstDayOfWeek).let {
            if (it.isAfter(this.date).not()) {
                it
            } else {
                it.minusDays(7)
            }
        }
        return (0..6).map {
            Day(firstDay.plusDays(it.toLong()))
        }.let(::ArrayList).let(::Week)
    }

    fun getMonth(): Month {
        val firstDay = this.date.withDayOfMonth(1)
        return (0 until this.date.lengthOfMonth()).map {
            Day(firstDay.plusDays(it.toLong()))
        }.let(::ArrayList).let(::Month)
    }

    fun isSameMonth(other: IDay): Boolean =
        this.date.month == other.date.month && this.date.year == other.date.year

    override fun compareTo(other: IDay): Int {
        return this.date.compareTo(other.date)
    }
}

interface IDaysCollection<D : IDay> {
    val days: ArrayList<D>
}

interface IWeek<D : IDay> : IDaysCollection<D> {
    val isCurrentWeek: Boolean
    val isCurrentMonth: Boolean
}

data class Week(
    override val days: ArrayList<Day>,
) : IWeek<Day> {
    companion object {
        val CurrentWeek: Week
            get() = Day.Today.getWeek()
    }

    // TODO use binary search
    override val isCurrentWeek: Boolean
        get() = days.any { it.isToday }

    override val isCurrentMonth: Boolean
        get() = days.any { it.isCurrentMonth }
}

interface IMonth<D : IDay, W : IWeek<out D>> : IDaysCollection<D> {
    val yearMonth: YearMonth
    fun getWeeks(firstDayOfWeek: DayOfWeek): List<W>
}

data class Month(
    override val days: ArrayList<Day>,
) : IMonth<Day, Week> {
    companion object {
        val CurrentMonth: Month
            get() = Day.Today.getMonth()
    }

    constructor(month: YearMonth) : this(month.atDay(1).let {
        (0 until it.lengthOfMonth()).map { offset ->
            Day(it.plusDays(offset.toLong()))
        }.let(::ArrayList)
    })

    val isCurrentMonth: Boolean
        get() = days.any { it.isCurrentMonth }

    override val yearMonth: YearMonth
        get() = YearMonth.from(days.first().date)

    fun getOverlappingMonth(firstDayOfWeek: DayOfWeek): OverlappingMonth {
        val firstDay = this.days.first().date
        val start = firstDay.with(firstDayOfWeek).let {
            if (it.isAfter(firstDay).not()) {
                it
            } else {
                it.minusDays(7)
            }
        }
        val lastDay = this.days.last().date
        val end = lastDay.with(DayOfWeek.of((firstDayOfWeek.value + 6) % 7)).let {
            if (it.isBefore(lastDay).not()) {
                it
            } else {
                it.plusDays(7)
            }
        }
        val prevDays = (0 until firstDay.toEpochDay() - start.toEpochDay()).map {
            Day(start.plusDays(it))
        }.let(::ArrayList)
        val nextDays = (1..end.toEpochDay() - lastDay.toEpochDay()).map {
            Day(end.plusDays(it))
        }.let(::ArrayList)
        return OverlappingMonth(this, prevDays, nextDays)
    }

    override fun getWeeks(firstDayOfWeek: DayOfWeek): List<Week> {
        return getOverlappingMonth(firstDayOfWeek).getWeeks(firstDayOfWeek)
    }
}

data class OverlappingMonth(
    val currentMonth: Month,
    val prevDays: ArrayList<Day>,
    val nextDays: ArrayList<Day>,
) : IMonth<Day, Week> by currentMonth {
    override val days: ArrayList<Day> = ArrayList(prevDays + currentMonth.days + nextDays)

    val firstDayOfWeek: DayOfWeek = days.first().date.dayOfWeek

    override fun getWeeks(firstDayOfWeek: DayOfWeek): List<Week> {
        return if (firstDayOfWeek == this.firstDayOfWeek) {
            days.chunked(7).map { Week(ArrayList(it)) }
        } else {
            currentMonth.getOverlappingMonth(firstDayOfWeek).getWeeks(firstDayOfWeek)
        }
    }
}