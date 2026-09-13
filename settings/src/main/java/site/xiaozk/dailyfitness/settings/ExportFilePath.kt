package site.xiaozk.dailyfitness.settings

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import kotlinx.io.files.Path

/** Prefix of the JSON file produced by a full-data export. */
const val EXPORT_FILE_PREFIX = "dailyfitness-export"

/** Extension of the JSON file produced by a full-data export. */
const val EXPORT_FILE_EXTENSION = "json"

/**
 * Builds the export file path inside the user-chosen [directory].
 *
 * The file name embeds [timestamp] down to the second so repeated exports never
 * silently overwrite each other, e.g. `dailyfitness-export-2024-05-06-081530.json`.
 * This is plain domain logic (no Android / `java.io`), testable on the JVM.
 */
fun exportFilePath(directory: Path, timestamp: LocalDateTime): Path {
    val date = timestamp.date
    val name = buildString {
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
    return Path(directory, name)
}
