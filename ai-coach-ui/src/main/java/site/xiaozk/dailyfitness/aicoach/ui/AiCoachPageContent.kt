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
import androidx.compose.material3.TextButton
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
import site.xiaozk.dailyfitness.aicoach.engine.CoachFailure
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessageContent
import site.xiaozk.dailyfitness.aicoach.engine.CoachSuggestion
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
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
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
                            is UiContent.TodayPlan ->
                                TodayPlanContent(content, onRefresh, onAdoptSuggestion)
                            is UiContent.NextAdvice ->
                                NextAdviceContent(content, onRefresh, onAdoptSuggestion)
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
                text = coachMessageText(message.content),
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
        CircularProgressIndicator()
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
private fun TodayPlanContent(
    content: UiContent.TodayPlan,
    onRefresh: () -> Unit,
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
) {
    Text(
        text = stringResource(R.string.ai_plan_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    content.parts.forEach { RecommendedPartCard(it, onAdoptSuggestion) }
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
private fun RecommendedPartCard(
    part: RecommendedPart,
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
) {
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
        part.actions.forEach { action ->
            ActionLine(
                action = action,
                onAdopt = {
                    onAdoptSuggestion(
                        CoachSuggestion(
                            partName = part.partName,
                            actionName = action.actionName,
                            sets = action.sets,
                            reps = action.reps,
                            weightKg = action.weightKg,
                            durationSec = action.durationSec,
                        )
                    )
                },
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
    }
}

@Composable
private fun ActionLine(action: RecommendedAction, onAdopt: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = actionLineText(action),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        TextButton(onClick = onAdopt) {
            Text(stringResource(R.string.ai_adopt))
        }
    }
}

@Composable
private fun NextAdviceContent(
    content: UiContent.NextAdvice,
    onRefresh: () -> Unit,
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
) {
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
    content.suggestions.firstOrNull()?.let { suggestion ->
        Button(
            onClick = { onAdoptSuggestion(suggestion) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.ai_adopt_to_add_set))
        }
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
    Text(text = failureText(state.failure), style = MaterialTheme.typography.bodyMedium)
    if (state.retryable) {
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ai_retry))
        }
    }
}

// ---------------------------------------------------------------- text helpers

@Composable
private fun coachMessageText(content: CoachMessageContent): String = when (content) {
    is CoachMessageContent.PlanRequest ->
        stringResource(R.string.ai_chat_plan_request, content.sessionsUsed)
    is CoachMessageContent.AdviceRequest ->
        stringResource(R.string.ai_chat_advice_request, content.setsToday, content.partName)
    is CoachMessageContent.PlanSummary -> planSummaryText(content.parts)
    is CoachMessageContent.AdviceSummary -> adviceSummaryText(content.advice)
}

@Composable
private fun planSummaryText(parts: List<RecommendedPart>): String {
    val partSeparator = stringResource(R.string.ai_summary_part_separator)
    val actionSeparator = stringResource(R.string.ai_summary_action_separator)
    val primarySuffix = stringResource(R.string.ai_summary_primary_suffix)
    val partColon = stringResource(R.string.ai_summary_part_colon)
    // `map` is inline, so composable helpers may be called in its lambda; the
    // non-inline joins run over the already-resolved strings.
    val actions = parts.map { part -> part.actions.map { actionLineText(it) } }
    return parts.mapIndexed { index, part ->
        val primary = if (part.isPrimary) primarySuffix else ""
        part.partName + primary + partColon + actions[index].joinToString(actionSeparator)
    }.joinToString(partSeparator)
}

@Composable
private fun adviceSummaryText(advice: Advice): String {
    val head = adviceTitle(advice)
    val params = buildList {
        if (advice.sets > 0) add(stringResource(R.string.ai_advice_params_sets, advice.sets))
        advice.reps?.let { add(stringResource(R.string.ai_action_reps, it)) }
        advice.weightKg?.let { add(stringResource(R.string.ai_action_weight, formatNumber(it))) }
        advice.durationSec?.let { add(stringResource(R.string.ai_advice_params_duration, it)) }
        advice.nextPartName?.let { add(stringResource(R.string.ai_advice_params_next_part, it)) }
    }.joinToString(" ")
    val reason = advice.reason?.let { stringResource(R.string.ai_advice_reason_inline, it) }.orEmpty()
    return listOf(head, params).filter { it.isNotBlank() }.joinToString(" ") + reason
}

@Composable
private fun actionLineText(action: RecommendedAction): String {
    val params = buildList {
        action.weightKg?.let { add(stringResource(R.string.ai_action_weight, formatNumber(it))) }
        action.reps?.let { add(stringResource(R.string.ai_action_reps, it)) }
        action.durationSec?.let { add(stringResource(R.string.ai_action_duration, it)) }
    }.joinToString(" ")
    val head = action.actionName + " · " + stringResource(R.string.ai_action_sets, action.sets)
    return listOf(head, params).filter { it.isNotBlank() }.joinToString(" ")
}

@Composable
private fun failureText(failure: CoachFailure): String = when (failure) {
    CoachFailure.InvalidKey -> stringResource(R.string.ai_error_invalid_key)
    CoachFailure.Network -> stringResource(R.string.ai_error_network)
    CoachFailure.RateLimited -> stringResource(R.string.ai_error_rate_limited)
    is CoachFailure.ModelError -> stringResource(R.string.ai_error_model, failure.detail)
    CoachFailure.NeedMoreWithoutHistory -> stringResource(R.string.ai_error_need_more_without_history)
    CoachFailure.InsufficientHistory -> stringResource(R.string.ai_error_insufficient_history)
    CoachFailure.EmptyPlan -> stringResource(R.string.ai_error_empty_plan)
    CoachFailure.PlanNotMatched -> stringResource(R.string.ai_error_plan_not_matched)
    CoachFailure.CannotDetermineTodayParts -> stringResource(R.string.ai_error_cannot_determine_today_parts)
    CoachFailure.CannotDetermineCurrentPart -> stringResource(R.string.ai_error_cannot_determine_current_part)
    CoachFailure.CurrentPartNotInLibrary -> stringResource(R.string.ai_error_current_part_not_in_library)
    CoachFailure.SuggestedActionNotInLibrary -> stringResource(R.string.ai_error_suggested_action_not_in_library)
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
