package site.xiaozk.dailyfitness.aicoach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import site.xiaozk.dailyfitness.aicoach.engine.Advice
import site.xiaozk.dailyfitness.aicoach.engine.AdviceKind
import site.xiaozk.dailyfitness.aicoach.engine.RecommendedAction
import site.xiaozk.dailyfitness.aicoach.engine.RecommendedPart
import site.xiaozk.dailyfitness.repository.model.AiCoachModel

/**
 * AI Coach tab content - purely presentational. The tab shell (top bar / bottom
 * navigation) lives in the app; this composable only renders the states produced
 * by [AiCoachViewModel].
 */
@Composable
fun AiCoachPageContent(
    state: AiCoachUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onRefresh: () -> Unit,
    onSaveConfig: (apiKey: String, model: AiCoachModel) -> Unit,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val setsToday = (state as? AiCoachUiState.Idle)?.setsToday
            ?: (state as? AiCoachUiState.Loading)?.setsToday
            ?: (state as? AiCoachUiState.Error)?.setsToday
            ?: 0
        if (state !is AiCoachUiState.Initial && state !is AiCoachUiState.ConfigMissing) {
            ScenarioHeader(setsToday)
        }
        when (state) {
            AiCoachUiState.Initial -> LoadingHint()
            AiCoachUiState.ConfigMissing -> ConfigForm(onSaveConfig)
            is AiCoachUiState.Idle -> when (val content = state.content) {
                null -> RefreshCallToAction(onRefresh)
                else -> when (content) {
                    UiContent.NoTrainParts -> EmptyContentHint()
                    is UiContent.TodayPlan -> TodayPlanContent(content, onRefresh)
                    is UiContent.NextAdvice -> NextAdviceContent(content, onRefresh)
                }
            }
            is AiCoachUiState.Loading -> LoadingHint()
            is AiCoachUiState.Error -> ErrorContent(state, onRefresh)
        }
    }
}

@Composable
private fun ScenarioHeader(setsToday: Int) {
    Text(
        text = if (setsToday <= 0) {
            stringResource(R.string.ai_status_today_empty)
        } else {
            stringResource(R.string.ai_status_today_sets, setsToday)
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun LoadingHint() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.width(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(stringResource(R.string.ai_loading))
    }
}

@Composable
private fun RefreshCallToAction(onRefresh: () -> Unit) {
    Text(
        text = stringResource(R.string.ai_ready_hint),
        style = MaterialTheme.typography.bodyMedium,
    )
    Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.ai_refresh))
    }
}

@Composable
private fun ConfigForm(onSaveConfig: (String, AiCoachModel) -> Unit) {
    Text(
        text = stringResource(R.string.ai_config_missing_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.ai_config_missing_hint),
        style = MaterialTheme.typography.bodyMedium,
    )
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(AiCoachModel.DeepSeekV4Flash) }
    OutlinedTextField(
        value = apiKey,
        onValueChange = { apiKey = it },
        label = { Text(stringResource(R.string.ai_api_key_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = stringResource(R.string.ai_model_label),
        style = MaterialTheme.typography.labelMedium,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AiCoachModel.entries.forEach { candidate ->
            FilterChip(
                selected = model == candidate,
                onClick = { model = candidate },
                label = { Text(stringResource(modelLabelRes(candidate))) },
            )
        }
    }
    Button(
        onClick = { onSaveConfig(apiKey, model) },
        enabled = apiKey.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.ai_save))
    }
}

@Composable
private fun TodayPlanContent(content: UiContent.TodayPlan, onRefresh: () -> Unit) {
    Text(
        text = stringResource(R.string.ai_plan_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    content.parts.forEach { RecommendedPartCard(it) }
    content.ignoredNames.takeIf { it.isNotEmpty() }?.let { ignored ->
        Text(
            text = stringResource(R.string.ai_ignored_hint, ignored.joinToString("、")),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
    OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.ai_refresh))
    }
}

@Composable
private fun RecommendedPartCard(part: RecommendedPart) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = part.partName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (part.isPrimary) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (part.isPrimary) R.string.ai_part_primary else R.string.ai_part_secondary
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        part.reason?.let {
            Text(
                text = stringResource(R.string.ai_reason_label, it),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        part.actions.forEach { ActionLine(it) }
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
    }
}

@Composable
private fun ActionLine(action: RecommendedAction) {
    Text(
        text = actionLineText(action),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(start = 8.dp),
    )
}

@Composable
private fun NextAdviceContent(content: UiContent.NextAdvice, onRefresh: () -> Unit) {
    Text(
        text = stringResource(R.string.ai_advice_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = adviceTitle(content.advice),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    content.advice.reason?.let {
        Text(
            text = stringResource(R.string.ai_advice_reason, it),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    content.ignoredNames.takeIf { it.isNotEmpty() }?.let { ignored ->
        Text(
            text = stringResource(R.string.ai_ignored_hint, ignored.joinToString("、")),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
    OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.ai_refresh))
    }
}

@Composable
private fun EmptyContentHint() {
    Text(
        text = stringResource(R.string.ai_no_train_parts_hint),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun ErrorContent(state: AiCoachUiState.Error, onRefresh: () -> Unit) {
    Text(
        text = stringResource(R.string.ai_error_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
    if (state.retryable) {
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ai_retry))
        }
    }
}

// ---------------------------------------------------------------- text helpers

private fun actionLineText(action: RecommendedAction): String {
    val params = mutableListOf<String>()
    if (action.weightKg != null) params += formatNumber(action.weightKg!!) + "kg"
    if (action.reps != null) params += "×" + action.reps
    if (action.durationSec != null) params += action.durationSec.toString() + "s"
    val paramsText = params.joinToString(" ")
    return "${action.actionName} · ${action.sets}组 ${paramsText}".trimEnd()
}

@Composable
private fun adviceTitle(advice: Advice): String = when (advice.kind) {
    AdviceKind.CONTINUE_CURRENT ->
        stringResource(R.string.ai_advice_continue_current, advice.actionName ?: "")
    AdviceKind.SWITCH_ACTION ->
        stringResource(R.string.ai_advice_switch_action, advice.actionName ?: "")
    AdviceKind.FINISH_PART -> stringResource(R.string.ai_advice_finish_part)
    AdviceKind.FINISH_DAY -> stringResource(R.string.ai_advice_finish_day)
}

private fun modelLabelRes(model: AiCoachModel): Int = when (model) {
    AiCoachModel.DeepSeekV4Flash -> R.string.ai_model_deepseek_v4_flash
    AiCoachModel.DeepSeekV4Pro -> R.string.ai_model_deepseek_v4_pro
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
