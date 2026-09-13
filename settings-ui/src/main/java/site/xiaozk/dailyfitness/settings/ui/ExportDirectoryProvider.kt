package site.xiaozk.dailyfitness.settings.ui

import kotlinx.io.files.Path

/**
 * App-provided capability used by the export screen to obtain a destination
 * directory from the user.
 *
 * The `:settings-ui` layer only knows this contract; the `:app` layer implements it
 * on top of the platform file manager (Storage Access Framework). Keeping the
 * platform picker out of the UI module lets the export flow be unit-tested with a
 * fake provider.
 */
interface ExportDirectoryProvider {
    /**
     * Asks the user to pick a directory through the system file manager.
     *
     * @return the chosen directory as a kotlinx-io [Path], or `null` if the user
     * cancelled or no directory picker is available.
     */
    suspend fun pickExportDirectory(): Path?
}
