package site.xiaozk.dailyfitness.aicoach.llm

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured (JSON) reply contracts of the two AI Coach flows.
 *
 * All numeric fields are nullable on purpose: a missing/odd value makes the
 * engine drop the item (or fail loudly) instead of silently trusting a default.
 */

/** Case A reply: today's part plan, or a request for more history. */
@Serializable
data class PartPlanReply(
    @property:LLMDescription("Whether more training history is needed for a reliable recommendation")
    val needMore: Boolean = false,
    @property:LLMDescription("When needMore=true: how many more complete training days are needed")
    val wantSessions: Int? = null,
    @property:LLMDescription("Recommendation plan when needMore=false, 1-2 parts")
    val plan: List<PartPlan>? = null,
)

@Serializable
data class PartPlan(
    @property:LLMDescription("Part name; must be copied verbatim from the exercise catalog")
    val partName: String,
    @property:LLMDescription("Whether this is the primary part")
    val isPrimary: Boolean = false,
    @property:LLMDescription("Short reason for the recommendation")
    val reason: String? = null,
    @property:LLMDescription("Recommended actions for this part, 1-4")
    val actions: List<ActionPlan> = emptyList(),
)

@Serializable
data class ActionPlan(
    @property:LLMDescription("Action name; must be copied verbatim from the exercise catalog")
    val actionName: String,
    @property:LLMDescription("Recommended number of sets, usually 3-5")
    val sets: Int? = null,
    @property:LLMDescription("Recommended reps per set (counted actions only)")
    val reps: Int? = null,
    @property:LLMDescription("Recommended weight in kg (weighted actions only)")
    val weightKg: Double? = null,
    @property:LLMDescription("Recommended duration in seconds (timed actions only)")
    val durationSec: Int? = null,
)

/** Case B reply: the single next-step advice. */
@Serializable
enum class AdviceKindReply {
    @SerialName("continue_current")
    @LLMDescription("Continue the current action with another set")
    CONTINUE_CURRENT,

    @SerialName("switch_action")
    @LLMDescription("Current action has enough sets; switch to another action for the same part")
    SWITCH_ACTION,

    @SerialName("finish_part")
    @LLMDescription("This part has had enough today; can switch parts")
    FINISH_PART,

    @SerialName("finish_day")
    @LLMDescription("Today's total volume is enough; recommend ending")
    FINISH_DAY,
}

@Serializable
data class NextAdviceReply(
    val kind: AdviceKindReply,
    @property:LLMDescription("Involved action name (for continue_current or switch_action); must come from the exercise catalog")
    val actionName: String? = null,
    @property:LLMDescription("Recommended additional/changed sets, usually 1-3")
    val sets: Int? = null,
    @property:LLMDescription("Recommended reps per set (counted actions only)")
    val reps: Int? = null,
    @property:LLMDescription("Recommended weight in kg (weighted actions only)")
    val weightKg: Double? = null,
    @property:LLMDescription("Recommended duration in seconds (timed actions only)")
    val durationSec: Int? = null,
    @property:LLMDescription("For finish_part: recommended next part name, must come from the exercise catalog")
    val nextPartName: String? = null,
    @property:LLMDescription("Short reason for the advice")
    val reason: String? = null,
)
