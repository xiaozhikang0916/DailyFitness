package site.xiaozk.dailyfitness.settings.ui

import kotlinx.io.Sink

/**
 * App-provided capability used by the export screen to obtain a destination for the
 * export file.
 *
 * The `:settings-ui` layer only knows this contract; the `:app` layer implements it
 * on top of the platform file manager (Storage Access Framework). Keeping the
 * platform picker out of the UI module lets the export flow be unit-tested with a
 * fake provider. It also means the platform layer can hand back a kotlinx-io [Sink]
 * backed by a `ContentResolver` stream, which is the only way to write into a
 * user-chosen directory under scoped storage.
 */
interface ExportDirectoryProvider {
    /**
     * Asks the user to pick an export directory through the system file manager and
     * creates a new file named [fileName] inside it.
     *
     * @return the writable target, or `null` if the user cancelled or the file could
     * not be created.
     */
    suspend fun createExportTarget(fileName: String): ExportTarget?
}

/**
 * A user-approved destination for the export.
 *
 * [displayPath] is the full path shown to the user (chosen directory + [fileName]).
 * [sink] is the kotlinx-io write side of the newly created file; the caller owns it
 * and must close it.
 */
data class ExportTarget(
    val displayPath: String,
    val sink: Sink,
)
