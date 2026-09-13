package site.xiaozk.dailyfitness.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import site.xiaozk.dailyfitness.repository.ISettingRepository
import site.xiaozk.dailyfitness.settings.exportFileName
import javax.inject.Inject

/**
 * State of the data-export screen.
 */
data class ExportDataUiState(
    /** A directory pick / file write is in flight. */
    val exporting: Boolean = false,
    /** Full path of the last successfully written file (shown to the user). */
    val exportedPath: String? = null,
    /** The last attempt failed (directory unavailable or write error). */
    val failed: Boolean = false,
)

/**
 * Drives a single full-data export.
 *
 * The destination comes from the app-injected [ExportDirectoryProvider] (the
 * platform folder picker, which also creates the file and opens a kotlinx-io
 * [kotlinx.io.Sink]), the file name from `:settings` domain logic ([exportFileName]),
 * and the bytes are written by [ISettingRepository]. The ViewModel therefore owns no
 * Android UI types and is unit-testable with fakes.
 */
@HiltViewModel
class ExportDataViewModel @Inject constructor(
    private val settingRepository: ISettingRepository,
    private val directoryProvider: ExportDirectoryProvider,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(ExportDataUiState())
    val state: StateFlow<ExportDataUiState> = _state.asStateFlow()

    /** Starts an export: pick a directory, then write the JSON file into it. */
    fun export() {
        if (_state.value.exporting) return
        viewModelScope.launch {
            _state.value = ExportDataUiState(exporting = true)

            val fileName = exportFileName(
                clock.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )
            val target = runCatching {
                directoryProvider.createExportTarget(fileName)
            }.getOrNull()
            if (target == null) {
                // Cancelled by the user (or the folder was unusable): back to idle.
                _state.value = ExportDataUiState()
                return@launch
            }

            val result = runCatching {
                // The provider owns the file; closing the sink persists the bytes.
                target.sink.use { sink ->
                    settingRepository.exportAllDataTo(sink)
                }
            }
            _state.value = ExportDataUiState(
                exporting = false,
                exportedPath = target.displayPath.takeIf { result.isSuccess },
                failed = result.isFailure,
            )
        }
    }
}
