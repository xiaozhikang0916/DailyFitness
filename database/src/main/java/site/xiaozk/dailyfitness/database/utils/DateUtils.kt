package site.xiaozk.dailyfitness.database.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant


/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */

/** First moment of this day in [zoneId]. */
fun LocalDate.getStartInstant(zoneId: TimeZone = TimeZone.currentSystemDefault()): Instant {
    return this.atStartOfDayIn(zoneId)
}

/**
 * Last moment of this day in [zoneId], meant to be used as an exclusive upper bound:
 * queries compare `recordTime < :to`, so a record exactly at midnight of the next day is
 * excluded while anything inside the day is included.
 */
fun LocalDate.getEndInstant(zoneId: TimeZone = TimeZone.currentSystemDefault()): Instant {
    return this.atStartOfDayIn(zoneId).plus(1.days).minus(1.nanoseconds)
}
