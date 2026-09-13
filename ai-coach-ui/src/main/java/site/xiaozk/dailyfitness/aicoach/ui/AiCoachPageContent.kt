package site.xiaozk.dailyfitness.aicoach.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
 *
 * The chat is a single flat [LazyColumn]: the last assistant bubble is either the
 * pending loading bubble or the real reply, and both share the same stable item id
 * so the loading bubble animates into the reply in place.
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
    val chatHistory = state.history

    // Keep the newest turn in view: on first entry and whenever a new message (e.g.
    // the pending bubble or the user turn) arrives. Wait for the first layout pass
    // so the item count is known before scrolling.
    LaunchedEffect(chatHistory.size, state is AiCoachUiState.Initial) {
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
            itemsIndexed(
                items = chatHistory,
                // Stable ids let the pending bubble and its real reply share one item,
                // so the message slides down (animateItem) and morphs in place.
                key = { index, message -> message.id ?: "msg-$index" },
            ) { _, message ->
                ChatBubble(
                    message = message,
                    onAdoptSuggestion = onAdoptSuggestion,
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item(key = "content-tail") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state) {
                    AiCoachUiState.Initial -> Unit
                    AiCoachUiState.ConfigMissing -> ConfigMissingHint(onOpenSettings)
                    is AiCoachUiState.Idle -> when {
                        state.content is UiContent.NoTrainParts -> EmptyContentHint()
                        // No assistant turn yet: the first-run call to action.
                        state.history.none { !it.fromUser } -> RefreshCallToAction(onRefresh)
                        else -> RefreshFooter(onRefresh)
                    }
                    is AiCoachUiState.Loading -> Unit // The pending assistant bubble is the loading affordance.
                    is AiCoachUiState.Error -> ErrorContent(state, onRefresh)
                }
            }
        }
    }
}

/** Bubble corner radii: large rounded sides, one pointed "tail" corner. */
private val BubbleCornerRadius = 20.dp
private val BubbleTailCornerRadius = 4.dp

/**
 * Material3-card-like speech bubble shape: large rounded corners everywhere except
 * the corner pointing at the sender side (top-start for left/assistant bubbles,
 * top-end for right/user bubbles), which is left pointed like a tail.
 */
private fun chatBubbleShape(fromUser: Boolean): RoundedCornerShape =
    if (fromUser) {
        RoundedCornerShape(
            topStart = BubbleCornerRadius,
            topEnd = BubbleTailCornerRadius,
            bottomEnd = BubbleCornerRadius,
            bottomStart = BubbleCornerRadius,
        )
    } else {
        RoundedCornerShape(
            topStart = BubbleTailCornerRadius,
            topEnd = BubbleCornerRadius,
            bottomEnd = BubbleCornerRadius,
            bottomStart = BubbleCornerRadius,
        )
    }

// Timing mirrors Material3's indeterminate CircularProgressIndicator (1.4.0):
// 6000ms loop, 1080 deg global rotation + stepped 4x90 deg extra rotation, and a
// highlighted sweep growing 0.1 -> 0.87 of the perimeter and back.
private const val BubbleBorderDurationMillis = 6000
private const val BubbleBorderGlobalRotationDegrees = 1080f
private const val BubbleBorderSweepMin = 0.1f
private const val BubbleBorderSweepMax = 0.87f

/** M3 `MotionTokens.EasingEmphasizedDecelerateCubicBezier`. */
private val BubbleBorderStepEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

/** M3 `MotionTokens.EasingStandardCubicBezier`. */
private val BubbleBorderSweepEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/**
 * Loading border: a highlighted segment of the bubble outline travels along the
 * exact [shape] contour (incl. the pointed tail corner) while a faint track stays
 * visible. Length and cycle follow Material3's circular loading animation.
 */
@Composable
private fun Modifier.bubbleLoadingBorder(shape: Shape): Modifier {
    val transition = rememberInfiniteTransition(label = "bubbleLoadingBorder")
    val globalRotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = BubbleBorderGlobalRotationDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BubbleBorderDurationMillis, easing = LinearEasing),
        ),
        label = "bubbleBorderGlobalRotation",
    )
    val additionalRotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = BubbleBorderDurationMillis
                90f at 300 using BubbleBorderStepEasing
                90f at 1500
                180f at 1800
                180f at 3000
                270f at 3300
                270f at 4500
                360f at 4800
                360f at 6000
            },
        ),
        label = "bubbleBorderAdditionalRotation",
    )
    val sweepFraction = transition.animateFloat(
        initialValue = BubbleBorderSweepMin,
        targetValue = BubbleBorderSweepMax,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = BubbleBorderDurationMillis
                BubbleBorderSweepMax at 3000 using BubbleBorderSweepEasing
                BubbleBorderSweepMin at 6000
            },
        ),
        label = "bubbleBorderSweep",
    )

    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val highlightColor = MaterialTheme.colorScheme.primary

    return drawWithCache {
        val strokeWidth = 2.dp.toPx()
        val inset = strokeWidth / 2f
        val insetSize = Size(
            width = (size.width - strokeWidth).coerceAtLeast(0f),
            height = (size.height - strokeWidth).coerceAtLeast(0f),
        )
        val outline = shape.createOutline(insetSize, layoutDirection, this)
        val path = Path().apply {
            when (outline) {
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Rectangle -> addRect(outline.rect)
                is Outline.Generic -> addPath(outline.path)
                else -> Unit
            }
            translate(Offset(inset, inset))
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length
        val segment = Path()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        onDrawWithContent {
            drawContent()
            if (length <= 0f) return@onDrawWithContent
            drawPath(path, color = trackColor, style = stroke)
            // Anchor the *leading* edge to the rotation and let the sweep trail behind
            // it, so shrinking pulls the tail forward instead of retracting the head.
            val leading = ((globalRotation.value + additionalRotation.value) / 360f * length) % length
            val sweep = (sweepFraction.value * length).coerceIn(0f, length)
            val trailing = (leading - sweep + length) % length
            segment.reset()
            if (trailing <= leading) {
                measure.getSegment(trailing, leading, segment, true)
            } else {
                measure.getSegment(trailing, length, segment, true)
                measure.getSegment(0f, leading, segment, true)
            }
            drawPath(segment, color = highlightColor, style = stroke)
        }
    }
}

@Composable
private fun ChatBubble(
    message: CoachMessage,
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = chatBubbleShape(message.fromUser)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (message.fromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = shape,
            // Hug up to 300dp and animate the size change (loading is tiny, the reply
            // is a full card).
            modifier = Modifier
                .widthIn(max = 300.dp)
                .animateContentSize(),
        ) {
            // Single AnimatedContent keeps one composition slot across
            // loading -> reply, which is what makes the morph animation possible.
            AnimatedContent(
                targetState = message.isLoading,
                modifier = if (message.isLoading) Modifier.bubbleLoadingBorder(shape) else Modifier,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ai-coach-bubble",
            ) { loading ->
                when {
                    loading -> LoadingBubbleContent()
                    else -> when (val content = message.content) {
                        is CoachMessageContent.PlanSummary ->
                            PlanResultContent(content, onAdoptSuggestion)
                        is CoachMessageContent.AdviceSummary ->
                            AdviceResultContent(content, message.suggestions, onAdoptSuggestion)
                        is CoachMessageContent.PlanRequest,
                        is CoachMessageContent.AdviceRequest ->
                            Text(
                                text = userMessageText(content),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                            )
                        CoachMessageContent.Loading -> LoadingBubbleContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBubbleContent() {
    // Text-only loading state; the animated border on the bubble carries the motion.
    Text(
        text = stringResource(R.string.ai_loading),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(10.dp),
    )
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
private fun RefreshFooter(onRefresh: () -> Unit) {
    OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
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

/** Rich plan reply, rendered inside the assistant bubble. */
@Composable
private fun PlanResultContent(
    content: CoachMessageContent.PlanSummary,
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
) {
    Column(modifier = Modifier.padding(10.dp)) {
        Text(
            text = stringResource(R.string.ai_plan_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        content.parts.forEach { RecommendedPartCard(it, onAdoptSuggestion) }
        IgnoredHint(content.ignoredNames)
    }
}

/** Rich next-step reply, rendered inside the assistant bubble. */
@Composable
private fun AdviceResultContent(
    content: CoachMessageContent.AdviceSummary,
    suggestions: List<CoachSuggestion>,
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
) {
    Column(modifier = Modifier.padding(10.dp)) {
        Text(
            text = stringResource(R.string.ai_advice_title),
            style = MaterialTheme.typography.titleSmall,
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
        IgnoredHint(content.ignoredNames)
        suggestions.firstOrNull()?.let { suggestion ->
            Button(
                onClick = { onAdoptSuggestion(suggestion) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                Text(stringResource(R.string.ai_adopt_to_add_set))
            }
        }
    }
}

@Composable
private fun IgnoredHint(names: List<String>) {
    names.takeIf { it.isNotEmpty() }?.let { ignored ->
        Text(
            text = stringResource(R.string.ai_ignored_hint, ignored.joinToString("、")),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun RecommendedPartCard(
    part: RecommendedPart,
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 6.dp),
        ) {
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
private fun userMessageText(content: CoachMessageContent): String = when (content) {
    is CoachMessageContent.PlanRequest ->
        stringResource(R.string.ai_chat_plan_request)
    is CoachMessageContent.AdviceRequest ->
        stringResource(R.string.ai_chat_advice_request, content.setsToday, content.partName)
    // Assistant replies and the loading placeholder are rendered as rich content.
    is CoachMessageContent.PlanSummary,
    is CoachMessageContent.AdviceSummary,
    CoachMessageContent.Loading -> ""
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
