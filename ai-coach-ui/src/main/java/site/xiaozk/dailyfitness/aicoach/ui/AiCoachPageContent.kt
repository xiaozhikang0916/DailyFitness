package site.xiaozk.dailyfitness.aicoach.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.material3.a2ui.A2uiSurfaceDefaults
import androidx.compose.runtime.Composable
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
import androidx.a2ui.model.processor.A2uiSurfaceModel
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
 * The screen is no longer a chat log: a training summary card sits on top and, below
 * it, only the latest assistant turn is shown (the recommended plan, the next-step
 * advice, the agent-authored surface, or the in-flight / failed state). User-side
 * request bubbles are never rendered - they exist only in the in-memory conversation
 * that the model sees.
 */
@Composable
fun AiCoachPageContent(
    state: AiCoachUiState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    a2uiSurfaces: List<A2uiSurfaceModel> = emptyList(),
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        // Every card is centered and never wider than [MessageCardMaxWidth].
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state is AiCoachUiState.Ready || state is AiCoachUiState.NoTrainParts) {
            TrainingSummaryCard(
                setsToday = state.setsToday,
                trainedParts = (state as? AiCoachUiState.Ready)?.trainedParts.orEmpty(),
                currentAction = (state as? AiCoachUiState.Ready)?.currentAction,
                currentActionSets = (state as? AiCoachUiState.Ready)?.currentActionSets ?: 0,
                onRefresh = onRefresh,
            )
        }
        AdviceSlot(
            state = state,
            a2uiSurfaces = a2uiSurfaces,
            onCancel = onCancel,
            onOpenSettings = onOpenSettings,
            onAdoptSuggestion = onAdoptSuggestion,
        )
    }
}

/** Corner radius shared by every card. */
private val MessageCardRadius = 20.dp

/** The card shape: four equal rounded corners. */
private val MessageCardShape = RoundedCornerShape(MessageCardRadius)

/** Widest a card or the trailing call-to-action may grow on large screens. */
private val MessageCardMaxWidth = 480.dp

/** Breathing room between a card's edge and its content. */
private val MessageCardPadding = 12.dp

/**
 * The top-of-page summary of today's training: how many sets were recorded, which
 * parts were trained and the action currently being performed.
 */
@Composable
private fun TrainingSummaryCard(
    setsToday: Int,
    trainedParts: List<String>,
    currentAction: String?,
    currentActionSets: Int,
    onRefresh: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MessageCardShape,
        modifier = Modifier
            .widthIn(max = MessageCardMaxWidth)
            .fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(MessageCardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.ai_training_summary_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onRefresh) {
                    Text(stringResource(R.string.ai_refresh))
                }
            }
            if (setsToday <= 0) {
                Text(
                    text = stringResource(R.string.ai_status_today_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.ai_status_today_sets, setsToday),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (trainedParts.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.ai_summary_parts, trainedParts.joinToString("、")),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                currentAction?.let {
                    Text(
                        text = stringResource(R.string.ai_summary_current_action, it, currentActionSets),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * The single advice slot below the summary: the latest assistant turn only.
 *
 * The user's own request turns are part of the in-memory conversation (they feed the
 * model) but are deliberately not rendered. The slot also carries the first-run hint,
 * the in-flight card with its cancel affordance, the failure hint and the gate hints for
 * missing config / empty library. The refresh action lives in the summary header instead.
 */
@Composable
private fun AdviceSlot(
    state: AiCoachUiState,
    a2uiSurfaces: List<A2uiSurfaceModel>,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = MessageCardMaxWidth)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            AiCoachUiState.Initial -> Unit
            AiCoachUiState.ConfigMissing -> ConfigMissingHint(onOpenSettings)
            AiCoachUiState.NoTrainParts -> EmptyContentHint()
            is AiCoachUiState.Ready -> {
                val latest = state.history.lastOrNull { !it.fromUser }
                if (latest == null) {
                    FirstRunHint()
                } else {
                    when (val content = latest.content) {
                        // The request side of a turn is never shown; only the reply is.
                        is CoachMessageContent.PlanRequest,
                        is CoachMessageContent.AdviceRequest -> Unit
                        is CoachMessageContent.PlanSummary,
                        is CoachMessageContent.AdviceSummary ->
                            ReplyCard(message = latest, onAdoptSuggestion = onAdoptSuggestion)
                        is CoachMessageContent.AgentUi ->
                            AgentUiTurn(content = content, surfaces = a2uiSurfaces)
                        CoachMessageContent.Loading -> LoadingCard(onCancel)
                        is CoachMessageContent.Failure -> FailureCard(content.failure)
                    }
                }
            }
        }
    }
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
 * Loading border: a highlighted segment of the outline travels along the exact [shape]
 * contour while a faint track stays visible. Length and cycle follow Material3's circular
 * loading animation.
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
private fun LoadingCard(onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MessageCardShape,
            modifier = Modifier
                .widthIn(max = MessageCardMaxWidth)
                .bubbleLoadingBorder(MessageCardShape),
        ) {
            LoadingBubbleContent()
        }
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.ai_cancel))
        }
    }
}

@Composable
private fun LoadingBubbleContent() {
    // Text-only loading state; the animated border on the card carries the motion.
    Text(
        text = stringResource(R.string.ai_loading),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(10.dp),
    )
}

/**
 * Renders the latest settled assistant reply (a recommended plan or next-step advice)
 * inside the app's own card.
 */
@Composable
private fun ReplyCard(
    message: CoachMessage,
    onAdoptSuggestion: (CoachSuggestion) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MessageCardShape,
        modifier = Modifier
            .widthIn(max = MessageCardMaxWidth)
            .fillMaxWidth(),
    ) {
        when (val content = message.content) {
            is CoachMessageContent.PlanSummary ->
                PlanResultContent(content, onAdoptSuggestion)
            is CoachMessageContent.AdviceSummary ->
                AdviceResultContent(content, message.suggestions, onAdoptSuggestion)
            // Only settled assistant replies reach this slot.
            else -> Unit
        }
    }
}

@Composable
private fun FailureCard(failure: CoachFailure) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MessageCardShape,
        modifier = Modifier
            .widthIn(max = MessageCardMaxWidth)
            .fillMaxWidth(),
    ) {
        Text(
            text = failureText(failure),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(MessageCardPadding),
        )
    }
}

/**
 * Renders an agent-authored turn: the A2UI surface the agent created under
 * [CoachMessageContent.AgentUi.surfaceId], wrapped in the app's own card.
 *
 * The outer card is app chrome (like every other turn), so it is deterministic and stays
 * consistent with the design system; the agent only composes what goes inside it. The
 * surface itself is looked up among the processor's active surfaces, so a turn stays a
 * pure function of the conversation plus the processor output.
 */
@Composable
private fun AgentUiTurn(
    content: CoachMessageContent.AgentUi,
    surfaces: List<A2uiSurfaceModel>,
    modifier: Modifier = Modifier,
) {
    val surface = surfaces.firstOrNull { it.id == content.surfaceId }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MessageCardShape,
            modifier = Modifier
                .widthIn(max = MessageCardMaxWidth)
                .fillMaxWidth(),
        ) {
            Box(modifier = Modifier.padding(MessageCardPadding)) {
                if (surface != null) {
                    A2uiSurface(
                        surfaceModel = surface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    // The turn is committed before the processor has applied its messages:
                    // this covers that one-frame gap (and a surface the agent deleted).
                    A2uiSurfaceDefaults.LoadingIndicator()
                }
            }
        }
    }
}

@Composable
private fun FirstRunHint() {
    Text(
        text = stringResource(R.string.ai_ready_hint),
        style = MaterialTheme.typography.bodyMedium,
    )
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

/** Rich plan reply, rendered inside the assistant card. */
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

/** Rich next-step reply, rendered inside the assistant card. */
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

// ---------------------------------------------------------------- text helpers

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
    CoachFailure.Timeout -> stringResource(R.string.ai_error_timeout)
    CoachFailure.Cancelled -> stringResource(R.string.ai_error_cancelled)
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
