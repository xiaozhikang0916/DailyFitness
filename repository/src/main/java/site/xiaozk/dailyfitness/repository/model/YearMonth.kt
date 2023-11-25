package site.xiaozk.dailyfitness.repository.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import java.time.Year
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
            this.month.value.compareTo(other.month.value)
        } else {
            this.year.compareTo(other.year)
        }
    }

    fun atDay(day: Int): LocalDate {
        return LocalDate(year, month, day)
    }

    fun lengthOfMonth(): Int {
        return month.length(Year.isLeap(year.toLong()))
    }

    override fun toString(): String {
        return "$year ${DefaultYearMonthFormatter.format(month)}"
    }

    fun atEndOfMonth(): LocalDate {
        return atDay(lengthOfMonth())
    }
}

fun YearMonth.toJavaYearMonth(): java.time.YearMonth {
    return java.time.YearMonth.of(year, month.value)
}
