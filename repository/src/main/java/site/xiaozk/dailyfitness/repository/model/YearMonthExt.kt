package site.xiaozk.dailyfitness.repository.model

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime

/**
 * Returns the current [YearMonth] in the system default time zone.
 */
fun YearMonth.Companion.now(): YearMonth {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return YearMonth(now.year, now.month)
}
