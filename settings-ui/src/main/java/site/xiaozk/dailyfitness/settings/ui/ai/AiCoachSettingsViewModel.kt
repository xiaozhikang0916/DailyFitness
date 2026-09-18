package site.xiaozk.dailyfitness.settings.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.repository.IAiCoachConfigStore
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import site.xiaozk.dailyfitness.repository.model.AiCoachModel
import javax.inject.Inject

/** Draft/loaded state of the AI Coach settings form. */
data class AiCoachSettingsUiState(
    /** False until the persisted config has been read (avoids overwriting it with defaults). */
    val loaded: Boolean = false,
    val apiKey: String = "",
    val model: AiCoachModel = AiCoachModel.DeepSeekV4Flash,
    val baseUrl: String = "",
    /** Kept as text so partial edits stay editable; parsed/validated via [timeoutValue]. */
    val timeoutSeconds: String = AiCoachConfig.DEFAULT_TIMEOUT_SECONDS.toString(),
    val saved: Boolean = false,
) {
    /** Parsed timeout, or null while the field is blank/invalid/out of range. */
    val timeoutValue: Int?
        get() = timeoutSeconds.trim().toIntOrNull()
            ?.takeIf { it in AiCoachConfig.MIN_TIMEOUT_SECONDS..AiCoachConfig.MAX_TIMEOUT_SECONDS }

    val canSave: Boolean
        get() = loaded && apiKey.isNotBlank() && timeoutValue != null
}

/**
 * Settings-screen ViewModel.
 *
 * Depends only on the `:repository` contract ([IAiCoachConfigStore]) - it has no
 * module dependency on `:ai-coach`. Saving writes through the same store the
 * `AiCoachConfigProvider` observes, so the engine picks the new config up reactively.
 */
@HiltViewModel
class AiCoachSettingsViewModel @Inject constructor(
    private val configStore: IAiCoachConfigStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AiCoachSettingsUiState())
    val state: StateFlow<AiCoachSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val config = configStore.observe().first()
            _state.update {
                it.copy(
                    loaded = true,
                    apiKey = config.apiKey,
                    model = config.model,
                    baseUrl = config.baseUrl.orEmpty(),
                    timeoutSeconds = config.timeoutSeconds.toString(),
                )
            }
        }
    }

    fun onApiKeyChange(value: String) {
        _state.update { it.copy(apiKey = value, saved = false) }
    }

    fun onModelChange(model: AiCoachModel) {
        _state.update { it.copy(model = model, saved = false) }
    }

    fun onBaseUrlChange(value: String) {
        _state.update { it.copy(baseUrl = value, saved = false) }
    }

    fun onTimeoutChange(value: String) {
        _state.update { it.copy(timeoutSeconds = value, saved = false) }
    }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        val timeoutSeconds = current.timeoutValue ?: return
        viewModelScope.launch {
            configStore.save(
                AiCoachConfig(
                    apiKey = current.apiKey.trim(),
                    model = current.model,
                    baseUrl = current.baseUrl.trim().takeIf { it.isNotBlank() },
                    timeoutSeconds = timeoutSeconds,
                )
            )
            _state.update { it.copy(saved = true) }
        }
    }
}
