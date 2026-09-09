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
    @property:LLMDescription("是否需要更多训练历史才能可靠推荐")
    val needMore: Boolean = false,
    @property:LLMDescription("needMore=true 时：还需要多少个完整的训练日（天数）")
    val wantSessions: Int? = null,
    @property:LLMDescription("needMore=false 时的推荐计划，1~2 个部位")
    val plan: List<PartPlan>? = null,
)

@Serializable
data class PartPlan(
    @property:LLMDescription("部位名，必须逐字来自动作库")
    val partName: String,
    @property:LLMDescription("是否主推部位")
    val isPrimary: Boolean = false,
    @property:LLMDescription("推荐理由（简短中文）")
    val reason: String? = null,
    @property:LLMDescription("该部位建议的动作，1~4 个")
    val actions: List<ActionPlan> = emptyList(),
)

@Serializable
data class ActionPlan(
    @property:LLMDescription("动作名，必须逐字来自动作库")
    val actionName: String,
    @property:LLMDescription("建议组数，通常 3~5")
    val sets: Int? = null,
    @property:LLMDescription("每组建议次数（仅计数类动作）")
    val reps: Int? = null,
    @property:LLMDescription("建议重量 kg（仅负重类动作）")
    val weightKg: Double? = null,
    @property:LLMDescription("建议时长秒（仅计时类动作）")
    val durationSec: Int? = null,
)

/** Case B reply: the single next-step advice. */
@Serializable
enum class AdviceKindReply {
    @SerialName("continue_current")
    @LLMDescription("继续当前动作做下一组")
    CONTINUE_CURRENT,

    @SerialName("switch_action")
    @LLMDescription("当前动作已够，换本部位另一动作")
    SWITCH_ACTION,

    @SerialName("finish_part")
    @LLMDescription("该部位今天已够，可换部位")
    FINISH_PART,

    @SerialName("finish_day")
    @LLMDescription("今天总量已够，建议结束")
    FINISH_DAY,
}

@Serializable
data class NextAdviceReply(
    val kind: AdviceKindReply,
    @property:LLMDescription("涉及的动作名（continue_current 或 switch_action 时），须来自动作库")
    val actionName: String? = null,
    @property:LLMDescription("建议再加/改做几组，通常 1~3")
    val sets: Int? = null,
    @property:LLMDescription("每组建议次数（仅计数类）")
    val reps: Int? = null,
    @property:LLMDescription("建议重量 kg（仅负重类）")
    val weightKg: Double? = null,
    @property:LLMDescription("建议时长秒（仅计时类）")
    val durationSec: Int? = null,
    @property:LLMDescription("finish_part 时建议的下一个部位名，须来自动作库")
    val nextPartName: String? = null,
    @property:LLMDescription("建议理由（简短中文）")
    val reason: String? = null,
)
