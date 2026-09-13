package site.xiaozk.dailyfitness.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.asSink
import kotlinx.io.buffered
import site.xiaozk.dailyfitness.settings.ui.ExportDirectoryProvider
import site.xiaozk.dailyfitness.settings.ui.ExportTarget
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * App-layer implementation of [ExportDirectoryProvider].
 *
 * The folder picker is a platform concern, so this class does not launch it
 * directly: `MainActivity` registers an `OpenDocumentTree` launcher and attaches it
 * via [attachLauncher]. The export ViewModel suspends in [createExportTarget] until
 * the user picks a folder (or cancels).
 *
 * A user-chosen directory is only writable through the Storage Access Framework
 * (scoped storage forbids raw file writes there), so this class creates the export
 * document via `DocumentsContract` and exposes its output stream as a kotlinx-io
 * [kotlinx.io.Sink] for `:settings` to write into.
 */
@Singleton
class AndroidExportDirectoryProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : ExportDirectoryProvider {

    private var launchPicker: (() -> Unit)? = null
    private var pending: CancellableContinuation<Uri?>? = null

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
        continuation.resume(uri)
    }

    override suspend fun createExportTarget(fileName: String): ExportTarget? {
        val treeUri = pickDirectory() ?: return null
        val resolver = context.contentResolver
        val parentDocument = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        // Creates "<fileName>" inside the chosen tree and returns its document URI.
        val documentUri = runCatching {
            DocumentsContract.createDocument(resolver, parentDocument, MIME_TYPE_JSON, fileName)
        }.getOrNull() ?: return null
        val output = resolver.openOutputStream(documentUri) ?: return null
        return ExportTarget(
            displayPath = "${treeUri.toDisplayDirectory()}/$fileName",
            sink = output.asSink().buffered(),
        )
    }

    private suspend fun pickDirectory(): Uri? = suspendCancellableCoroutine { continuation ->
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

    private companion object {
        const val MIME_TYPE_JSON = "application/json"
    }
}

/** Human-readable directory name derived from the chosen tree URI (for the success message). */
private fun Uri.toDisplayDirectory(): String {
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(this) }.getOrNull()
        ?: return toString()
    val separator = documentId.indexOf(':')
    return if (separator >= 0) documentId.substring(separator + 1) else documentId
}
