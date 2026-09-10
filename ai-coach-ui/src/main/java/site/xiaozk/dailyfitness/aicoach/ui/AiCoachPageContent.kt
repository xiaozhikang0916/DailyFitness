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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import site.xiaozk.dailyfitness.aicoach.engine.Advice
import site.xiaozk.dailyfitness.aicoach.engine.AdviceKind
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.aicoach.engine.RecommendedAction
import site.xiaozk.dailyfitness.aicoach.engine.RecommendedPart

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
    onOpenSettings: () -> Unit,
) {
    val listState = rememberLazyListState()
    // When the latest result is rendered as a structured card, the trailing
    // assistant message would be duplicated; hide it from the chat list.
    val latestContent = (state as? AiCoachUiState.Idle)?.content
    val history = state.conversation
    val chatHistory = if (
        (latestContent is UiContent.TodayPlan || latestContent is UiContent.NextAdvice) &&
        history.lastOrNull()?.fromUser == false
    ) {
        history.dropLast(1)
    } else {
        history
    }

    // Keep the newest turn/result in view: on first entry and whenever a new
    // message or structured result arrives. Wait for the first layout pass so the
    // item count is known before scrolling.
    LaunchedEffect(chatHistory.size, latestContent, state is AiCoachUiState.Initial) {
        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .filter { it > 0 }
            .first()
        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
    }

    val setsToday = (state as? AiCoachUiState.Idle)?.setsToday
        ?: (state as? AiCoachUiState.Loading)?.setsToday
        ?: (state as? AiCoachUiState.Error)?.setsToday
        ?: 0

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state !is AiCoachUiState.Initial && state !is AiCoachUiState.ConfigMissing) {
            item(key = "scenario-header") { ScenarioHeader(setsToday) }
        }
        if (chatHistory.isNotEmpty()) {
            item(key = "chat-history-title") {
                Text(
                    text = stringResource(R.string.ai_chat_history_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            items(chatHistory) { message -> ChatBubble(message) }
        }
        item(key = "content-tail") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state) {
                    AiCoachUiState.Initial -> LoadingHint()
                    AiCoachUiState.ConfigMissing -> ConfigMissingHint(onOpenSettings)
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
    }
}

@Composable
private fun ChatBubble(message: CoachMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (message.fromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.width(300.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp),
            )
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
private fun ConfigMissingHint(onOpenSettings: () -> Unit) {
    Text(
        text = stringResource(R.string.ai_config_missing_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.ai_config_missing_hint),
        style = MaterialTheme.typography.bodyMedium,
    )
    Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.ai_open_settings))
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

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
