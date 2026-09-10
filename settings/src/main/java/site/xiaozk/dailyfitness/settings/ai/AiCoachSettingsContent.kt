package site.xiaozk.dailyfitness.settings.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import site.xiaozk.dailyfitness.repository.model.AiCoachModel
import site.xiaozk.dailyfitness.settings.R

/**
 * AI Coach settings form - purely presentational.
 *
 * Owned by `:settings` but rendered by the app inside its `SubPageScaffold`, so it
 * receives the scaffold padding and emits edit/save callbacks. It only knows the
 * `:repository` models, never `:ai-coach`.
 */
@Composable
fun AiCoachSettingsContent(
    state: AiCoachSettingsUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onApiKeyChange: (String) -> Unit,
    onModelChange: (AiCoachModel) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.ai_settings_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = onApiKeyChange,
            label = { Text(stringResource(R.string.ai_settings_api_key)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.ai_settings_model),
            style = MaterialTheme.typography.labelMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiCoachModel.entries.forEach { candidate ->
                FilterChip(
                    selected = state.model == candidate,
                    onClick = { onModelChange(candidate) },
                    label = { Text(stringResource(modelLabelRes(candidate))) },
                )
            }
        }
        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text(stringResource(R.string.ai_settings_base_url)) },
            supportingText = { Text(stringResource(R.string.ai_settings_base_url_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.ai_settings_save))
        }
        if (state.saved) {
            Text(
                text = stringResource(R.string.ai_settings_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (state.loaded && state.apiKey.isBlank()) {
            Text(
                text = stringResource(R.string.ai_settings_api_key_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun modelLabelRes(model: AiCoachModel): Int = when (model) {
    AiCoachModel.DeepSeekV4Flash -> R.string.ai_settings_model_flash
    AiCoachModel.DeepSeekV4Pro -> R.string.ai_settings_model_pro
}
