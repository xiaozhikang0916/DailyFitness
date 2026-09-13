package site.xiaozk.dailyfitness.settings

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number

/** Prefix of the JSON file produced by a full-data export. */
const val EXPORT_FILE_PREFIX = "dailyfitness-export"

/** Extension of the JSON file produced by a full-data export. */
const val EXPORT_FILE_EXTENSION = "json"

/**
 * Builds the file name used for a full-data export.
 *
 * It embeds [timestamp] down to the second so repeated exports never silently
 * overwrite each other, e.g. `dailyfitness-export-2024-05-06-081530.json`.
 * This is plain domain logic (no Android / `java.io`), testable on the JVM, and is
 * reused by the app layer to append the name to the user-chosen directory.
 */
fun exportFileName(timestamp: LocalDateTime): String {
    val date = timestamp.date
    return buildString {
        append(EXPORT_FILE_PREFIX)
        append('-')
        append(date.year)
        append('-')
        append(date.month.number.toString().padStart(2, '0'))
        append('-')
        append(date.day.toString().padStart(2, '0'))
        append('-')
        append(timestamp.hour.toString().padStart(2, '0'))
        append(timestamp.minute.toString().padStart(2, '0'))
        append(timestamp.second.toString().padStart(2, '0'))
        append('.')
        append(EXPORT_FILE_EXTENSION)
    }
}
