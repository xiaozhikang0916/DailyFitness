package site.xiaozk.dailyfitness.repository.model

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaMonth
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.number
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * @author: xiaozhikang
 * @create: 2023/11/25
 */
@Serializable
data class YearMonth(
    val year: Int,
    val month: Month,
) : Comparable<YearMonth> {
    companion object {
        fun now(): YearMonth {
            return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).let {
                YearMonth(it.year, it.month)
            }

        }

        private val DefaultYearMonthFormatter: DateTimeFormatter
            get() = DateTimeFormatterBuilder()
                .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL)
                .toFormatter(Locale.getDefault())
    }

    constructor(date: LocalDate) : this(date.year, date.month)

    override fun compareTo(other: YearMonth): Int {
        return if (this.year == other.year) {
            this.month.number.compareTo(other.month.number)
        } else {
            this.year.compareTo(other.year)
        }
    }

    fun atDay(day: Int): LocalDate {
        return LocalDate(year, month, day)
    }

    fun lengthOfMonth(): Int {
        return when (month) {
            Month.FEBRUARY -> if (year.isLeap()) 29 else 28
            Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
            else -> 31
        }
    }

    override fun toString(): String {
        return "$year ${DefaultYearMonthFormatter.format(month.toJavaMonth())}"
    }

    fun atEndOfMonth(): LocalDate {
        return atDay(lengthOfMonth())
    }

    fun previousMonth(): YearMonth {
        return if (month.number == 1) {
            YearMonth(year - 1, Month(12))
        } else {
            YearMonth(year, Month(month.number - 1))
        }
    }

    fun nextMonth(): YearMonth {
        return if (month.number == 12) {
            YearMonth(year + 1, Month(1))
        } else {
            YearMonth(year, Month(month.number + 1))
        }
    }
}

private fun Int.isLeap(): Boolean =
    this % 4 == 0 && (this % 100 != 0 || this % 400 == 0)
