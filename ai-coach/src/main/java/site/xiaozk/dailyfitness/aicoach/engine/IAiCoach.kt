package site.xiaozk.dailyfitness.aicoach.engine

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Facade of the AI Coach feature, consumed by the UI module (:ai-coach-ui).
 *
 * [recommendToday] decides between:
 * - Case A: today has no workout records yet -> a full follow-along plan for today;
 * - Case B: today already has records -> advice for the next step.
 *
 * The engine is stateless: the caller (session-scoped ViewModel) passes its
 * in-memory conversation [history] and receives the new turn(s) inside the
 * successful results so it can append them.
 */
interface IAiCoach {
    suspend fun recommendToday(history: List<CoachMessage> = emptyList()): AiCoachResult
}

/**
 * One in-memory conversation message (never persisted).
 *
 * [content] is a UI-agnostic descriptor: the engine never hardcodes display text,
 * the UI layer resolves it to localized string resources.
 */
data class CoachMessage(
    val fromUser: Boolean,
    val content: CoachMessageContent,
    /** Structured suggestions carried by assistant turns, for M3.3 one-click adopt/prefill. */
    val suggestions: List<CoachSuggestion> = emptyList(),
    val at: Instant = Clock.System.now(),
)

/**
 * A machine-actionable recommendation (action/weight/reps, plus part/sets/duration
 * so timed actions and multi-action plans can be prefilled as well).
 *
 * `@Serializable` so it can travel as a navigation argument (M3.3 prefill).
 */
@Serializable
data class CoachSuggestion(
    val partName: String? = null,
    val actionName: String,
    val sets: Int? = null,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSec: Int? = null,
)

sealed interface AiCoachResult {
    /** Case A: today's recommended plan (parts -> actions -> sets/weight/reps/duration). */
    data class TodayPlan(
        val sessionsUsed: Int,
        val rounds: Int,
        val parts: List<RecommendedPart>,
        val ignoredNames: List<String>,
        /** Turn produced by this request; caller appends it to its conversation. */
        val newMessages: List<CoachMessage> = emptyList(),
    ) : AiCoachResult

    /** Case B: next-step advice. */
    data class NextAdvice(
        val advice: Advice,
        val ignoredNames: List<String> = emptyList(),
        /** Turn produced by this request; caller appends it to its conversation. */
        val newMessages: List<CoachMessage> = emptyList(),
    ) : AiCoachResult

    /** AI key/model not configured yet. */
    data object ConfigMissing : AiCoachResult

    /** No parts/actions exist in the library at all. */
    data object NoTrainParts : AiCoachResult

    data class Failed(
        val failure: CoachFailure,
        val retryable: Boolean,
    ) : AiCoachResult
}

data class RecommendedPart(
    val partName: String,
    val isPrimary: Boolean,
    val reason: String?,
    val actions: List<RecommendedAction>,
)

data class RecommendedAction(
    val actionName: String,
    val sets: Int,
    val reps: Int?,
    val weightKg: Double?,
    val durationSec: Int?,
)

enum class AdviceKind {
    /** Keep doing the current action: carry weight/reps/duration into the next set. */
    CONTINUE_CURRENT,

    /** Enough sets of the current action; switch to another action of the same part. */
    SWITCH_ACTION,

    /** This part is done for today; (optionally) suggested next part to train. */
    FINISH_PART,

    /** Today's total volume is enough; stop training. */
    FINISH_DAY,
}

data class Advice(
    val kind: AdviceKind,
    val actionName: String?,
    val sets: Int,
    val reps: Int?,
    val weightKg: Double?,
    val durationSec: Int?,
    val nextPartName: String?,
    val reason: String?,
)

