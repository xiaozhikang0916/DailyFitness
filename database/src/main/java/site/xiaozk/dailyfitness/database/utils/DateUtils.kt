package site.xiaozk.dailyfitness.database.utils

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.toDuration


/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */

fun LocalDate.getStartEpochMillis(zoneId: TimeZone = TimeZone.currentSystemDefault()): Long {
    return this.atStartOfDayIn(zoneId).toEpochMilliseconds()
}

fun LocalDate.getEndEpochMillis(zoneId: TimeZone = TimeZone.currentSystemDefault()): Long {
    return this.atStartOfDayIn(zoneId).plus(1.days).minus(1.nanoseconds).toEpochMilliseconds()
}