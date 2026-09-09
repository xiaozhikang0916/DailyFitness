package site.xiaozk.dailyfitness.aicoach.prompt

import site.xiaozk.dailyfitness.repository.model.TrainPartGroup

/**
 * Pure prompt text builders for the two recommendation flows.
 *
 * Kept free of any koog import so the engine and prompt logic are unit-testable
 * without an LLM; KoogPlanExecutor only wraps the returned text into koog Prompts.
 */
object AiPrompts {

    private val PART_PLAN_SYSTEM = """
你是用户的私人健身教练。用户采用 4 分化训练：每天练 1~2 个部位，通常 4 次训练完成一个循环。
你的任务：根据【训练历史】与【动作库】，推荐今天应训练的部位，并给出可直接照着练的完整计划。

硬性规则：
1. 部位名与动作名必须逐字复制自【动作库】，禁止自造、缩写或近似名称。
2. 只能推荐【动作库】中真实存在的部位与动作。
3. 数值单位：重量一律 kg，次数为整数次，时长为整数秒。
4. 动作标注“负重/计数/计时”决定可填字段：负重→weightKg；计数→reps；计时→durationSec。不具备该能力的字段不要填。
5. 每个动作给 3~5 组（sets）；若为首次训练或久未训练可适当降低。
6. 正常情况 recommend 1~2 个部位，主推部位 isPrimary=true，另一个为备选；每个部位给 1~4 个动作。
7. 若判定历史不足以给出可靠建议：needMore=true 并给出 wantSessions=还需要多少个完整训练日；此时不要填 plan。
8. 若历史已足够（或已明确告知只能提供这些历史）：needMore=false，必须填 plan。
9. reason 用一两句简短中文说明推荐理由。
""".trimIndent()

    private val NEXT_ADVICE_SYSTEM = """
你是用户的私人健身教练。用户正在训练，你负责判断下一步该做什么。
输入包含：【今日已练内容】【该部位近期历史】以及【动作库】。
请你从下面四种动作中选且只选一种：
- continue_current：当前动作继续做下一组 → 填 actionName(当前动作) + sets(建议再加几组,通常1~3) + weightKg/reps 或 durationSec（按动作类型，基于上一组微调，一般不超过上一组重量+2.5kg 的递增）；
- switch_action：当前动作组数已够，建议换同部位的另一个动作 → actionName 填新动作名 + 新动作的建议 sets/reps/weightKg/durationSec；
- finish_part：该部位今天训练量已够 → 可填 nextPartName 建议接下来练的部位（须来自动作库；不确定可不填）；
- finish_day：今天总量已够，建议结束训练。

硬性规则：
1. 所有部位名/动作名必须逐字来自【动作库】。
2. 单位：重量 kg、次数整数次、时长整数秒；只填动作类型允许的字段。
3. reason 用一两句中文解释（如训练量、间隔天数、渐进超负荷等）。
""".trimIndent()

    fun partPlanSystem(): String = PART_PLAN_SYSTEM

    fun nextAdviceSystem(): String = NEXT_ADVICE_SYSTEM

    /** 【动作库】section text, names only (+ type markers). Groups come straight from the repository models. */
    fun formatCatalog(groups: List<TrainPartGroup>): String = buildString {
        appendLine("【动作库】（只能从这里选名称）")
        groups.forEach { group ->
            val actions = group.actions.joinToString("、") { action ->
                action.actionName + actionTypeMark(action.isWeightedAction, action.isCountedAction, action.isTimedAction)
            }
            appendLine("- ${group.part.partName}: ${actions}")
        }
    }.trimEnd()

    /** One session block, e.g. "3天前(2025-01-05)" + parts/actions/sets. */
    fun formatSessionDay(session: SessionSummary, orderLabel: String? = null): String = buildString {
        val header = buildString {
            if (!orderLabel.isNullOrBlank()) append("$orderLabel ")
            append("${session.daysAgo}天前(${session.date})")
        }
        appendLine(header)
        session.parts.forEach { part ->
            appendLine("  部位[${part.partName}]")
            part.actions.forEach { action ->
                appendLine("    - ${action.actionName}${actionTypeMark(action)}")
                val suffix = if (action.truncated) "（共${action.sets.size}组以上，仅列前${MAX_SETS_PER_ACTION}组）" else ""
                action.sets.forEach { set ->
                    appendLine("      ${formatSet(action, set)}")
                }
                if (suffix.isNotEmpty()) appendLine("      $suffix")
            }
        }
    }

    fun formatHistory(sessions: List<SessionSummary>): String = buildString {
        appendLine("【训练历史】共 ${sessions.size} 个训练日")
        sessions.forEachIndexed { index, session ->
            append(formatSessionDay(session, orderLabel = "第${sessions.size - index}次"))
        }
    }.trimEnd()

    /** Case A user prompt. [additionalNote] carries hard cap / fallback instructions. */
    fun partPlanUser(
        groups: List<TrainPartGroup>,
        history: List<SessionSummary>,
        additionalNote: String? = null,
    ): String = buildString {
        appendLine(formatCatalog(groups))
        appendLine()
        if (history.isEmpty()) {
            appendLine("【训练历史】空：没有可参考的历史记录。请直接基于【动作库】给出合理的首次推荐计划；")
            appendLine("不要设置 needMore（没有更多历史可提供），必须返回完整 plan。")
        } else {
            appendLine(formatHistory(history))
        }
        if (!additionalNote.isNullOrBlank()) {
            appendLine()
            appendLine("【补充说明】$additionalNote")
        }
    }.trimEnd()

    /** Case B user prompt. */
    fun nextAdviceUser(
        groups: List<TrainPartGroup>,
        todaySession: SessionSummary,
        partHistory: List<SessionSummary>,
        lastPartDaysAgo: Int?,
    ): String = buildString {
        appendLine(formatCatalog(groups))
        appendLine()
        appendLine("【今日已练内容】")
        appendLine(formatSessionDay(todaySession, orderLabel = "今天"))
        appendLine()
        if (partHistory.isEmpty()) {
            if (lastPartDaysAgo == null) {
                appendLine("【该部位近期历史】空：今天之前从未练过该部位，请主要依据今日已练内容与动作库判断。")
            } else {
                appendLine("【该部位近期历史】空（该部位最近一次训练在 ${lastPartDaysAgo} 天前，但不在下方历史窗口中）。")
            }
        } else {
            appendLine("【该部位近期历史】距上次练该部位：${lastPartDaysAgo}天前")
            appendLine(formatHistory(partHistory))
        }
    }.trimEnd()

    private fun formatSet(action: ActionSummary, set: SetSummary): String {
        val parts = mutableListOf<String>()
        if (action.isWeightedAction && set.weightKg != null) {
            parts += formatNumber(set.weightKg) + "kg"
        }
        if (action.isCountedAction && set.reps != null) {
            parts += "×" + set.reps
        }
        if (action.isTimedAction && set.durationSec != null) {
            parts += formatNumber(set.durationSec.toDouble()) + "秒"
        }
        if (parts.isEmpty()) parts += "(无参数记录)"
        return parts.joinToString("")
    }

    private fun formatNumber(value: Double): String {
        val rounded = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        return rounded
    }

    private fun actionTypeMark(action: ActionSummary): String =
        actionTypeMark(action.isWeightedAction, action.isCountedAction, action.isTimedAction)

    private fun actionTypeMark(weighted: Boolean, counted: Boolean, timed: Boolean): String {
        val marks = buildList {
            if (weighted) add("负重")
            if (counted) add("计数")
            if (timed) add("计时")
        }
        return if (marks.isEmpty()) "(无参数)" else "(${marks.joinToString("+")})"
    }
}
