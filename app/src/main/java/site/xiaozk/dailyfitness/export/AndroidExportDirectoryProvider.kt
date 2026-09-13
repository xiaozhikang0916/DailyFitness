package site.xiaozk.dailyfitness.export

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.files.Path
import site.xiaozk.dailyfitness.settings.ui.ExportDirectoryProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * App-layer implementation of [ExportDirectoryProvider].
 *
 * The actual folder picker is a platform concern, so this class does not launch it
 * directly: `MainActivity` registers an `OpenDocumentTree` launcher and attaches it
 * via [attachLauncher]. The export ViewModel simply suspends in
 * [pickExportDirectory] until the user picks a folder (or cancels).
 *
 * The chosen Storage Access Framework tree URI is mapped back to a kotlinx-io
 * [Path] so the `:settings` layer can write with `SystemFileSystem` (no `java.io`).
 */
@Singleton
class AndroidExportDirectoryProvider @Inject constructor() : ExportDirectoryProvider {

    private var launchPicker: (() -> Unit)? = null
    private var pending: CancellableContinuation<Path?>? = null

    /** Called by the Activity with a callback that launches the system folder picker. */
    fun attachLauncher(launch: () -> Unit) {
        launchPicker = launch
    }

    fun detachLauncher() {
        launchPicker = null
    }

    /** Called by the Activity with the picker result (`null` when the user cancelled). */
    fun onDirectoryPicked(uri: Uri?) {
        val continuation = pending ?: return
        pending = null
        continuation.resume(uri?.toFileSystemPath())
    }

    override suspend fun pickExportDirectory(): Path? = suspendCancellableCoroutine { continuation ->
        val launch = launchPicker
        if (launch == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        // Only one picker can be pending at a time.
        pending?.cancel()
        pending = continuation
        continuation.invokeOnCancellation {
            if (pending === continuation) pending = null
        }
        launch()
    }
}

/**
 * Maps a `DocumentsContract` tree URI to a real filesystem [Path].
 *
 * Only the two volumes that expose a plain filesystem path are supported:
 * `primary` (shared external storage) and `raw` (path already absolute). Other
 * document providers (cloud, USB, …) do not map to a [Path] and yield `null`.
 */
@Suppress("DEPRECATION")
private fun Uri.toFileSystemPath(): Path? {
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(this) }.getOrNull()
        ?: return null
    val separator = documentId.indexOf(':')
    if (separator < 0) return null
    val volume = documentId.substring(0, separator)
    val relative = documentId.substring(separator + 1)
    return when {
        volume.equals("primary", ignoreCase = true) -> {
            val root = Environment.getExternalStorageDirectory().absolutePath
            if (relative.isBlank()) Path(root) else Path(root, relative)
        }
        volume.equals("raw", ignoreCase = true) ->
            relative.takeIf { it.isNotBlank() }?.let { Path(it) }
        else -> null
    }
}
