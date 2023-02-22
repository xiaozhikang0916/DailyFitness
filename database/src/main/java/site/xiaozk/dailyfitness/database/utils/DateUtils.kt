package site.xiaozk.dailyfitness.database.utils

import java.time.LocalDate
import java.time.ZoneId

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */

fun LocalDate.getStartEpochMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return this.atStartOfDay(zoneId).toEpochSecond() * 1000
}

fun LocalDate.getEndEpochMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return this.atStartOfDay(zoneId).plusDays(1).minusNanos(1).toEpochSecond() * 1000
}