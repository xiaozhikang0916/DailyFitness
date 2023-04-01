package site.xiaozk.dailyfitness.utils

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

/**
 * @author: xiaozhikang
 * @create: 2023/3/31
 */

fun getLocalDateTimeFormatter(locale: Locale) = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .append(DateTimeFormatter.ISO_LOCAL_DATE)
    .appendLiteral(' ')
    .append(DateTimeFormatter.ISO_LOCAL_TIME).toFormatter(locale)


fun getLocalDateFormatter() = DateTimeFormatter.ISO_LOCAL_DATE
