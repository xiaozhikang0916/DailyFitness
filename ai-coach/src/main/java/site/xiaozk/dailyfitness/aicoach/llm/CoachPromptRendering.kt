package site.xiaozk.dailyfitness.aicoach.llm

import site.xiaozk.dailyfitness.aicoach.engine.Advice
import site.xiaozk.dailyfitness.aicoach.engine.AdviceKind
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessageContent
import site.xiaozk.dailyfitness.aicoach.engine.RecommendedAction

/**
 * Renders a conversation turn for the LLM history.
 *
 * This is **prompt-side** text sent to the model, never shown to the user: the
 * display descriptors live in [CoachMessageContent] and the UI layer owns all
 * user-facing copy. Keeping the wording here makes that boundary explicit.
 */
internal fun CoachMessageContent.toPromptText(): String = when (this) {
    is CoachMessageContent.PlanRequest ->
        "[$date][Case A] 今日尚无锻炼记录；基于训练历史请求推荐"

    is CoachMessageContent.AdviceRequest ->
        "[$date][Case B] 今日已练 $setsToday 组，当前部位：$partName；请求下一步建议"

    is CoachMessageContent.PlanSummary ->
        parts.joinToString("；") { part ->
            val primary = if (part.isPrimary) "（主推）" else ""
            part.partName + primary + "：" + part.actions.joinToString("、") { it.toPromptText() }
        }

    is CoachMessageContent.AdviceSummary -> advice.toPromptText()

    // Transient UI placeholder: never part of the request history, so it renders
    // to nothing if it ever leaks here.
    CoachMessageContent.Loading -> ""
}

private fun RecommendedAction.toPromptText(): String = buildList {
    add(actionName)
    add("${sets}组")
    reps?.let { add("×$it") }
    weightKg?.let { add("@${formatNumber(it)}kg") }
    durationSec?.let { add("${it}秒") }
}.joinToString(" ")

private fun Advice.toPromptText(): String {
    val head = when (kind) {
        AdviceKind.CONTINUE_CURRENT -> "继续${actionName ?: "当前动作"}"
        AdviceKind.SWITCH_ACTION -> "换练${actionName ?: "?（同部位其他动作）"}"
        AdviceKind.FINISH_PART -> "该部位今天已够"
        AdviceKind.FINISH_DAY -> "今天总量已够，建议收工"
    }
    val params = buildList {
        if (sets > 0) add("${sets}组")
        reps?.let { add("×$it") }
        weightKg?.let { add("@${formatNumber(it)}kg") }
        durationSec?.let { add("每组${it}秒") }
        nextPartName?.let { add("下一步建议部位：$it") }
    }.joinToString(" ")
    val reason = reason?.let { "；理由：$it" }.orEmpty()
    return listOf(head, params).filter { it.isNotBlank() }.joinToString(" ") + reason
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
