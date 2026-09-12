package site.xiaozk.dailyfitness.aicoach.prompt

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
 *
 * Labels are English; part/action names are user data and stay verbatim.
 */
internal fun CoachMessageContent.toPromptText(): String = when (this) {
    is CoachMessageContent.PlanRequest ->
        "[$date][Case A] No workout recorded today; requesting a recommendation from training history"

    is CoachMessageContent.AdviceRequest ->
        "[$date][Case B] $setsToday sets done today, current part: $partName; requesting the next-step advice"

    is CoachMessageContent.PlanSummary ->
        parts.joinToString("; ") { part ->
            val primary = if (part.isPrimary) " (primary)" else ""
            part.partName + primary + ": " + part.actions.joinToString(", ") { it.toPromptText() }
        }

    is CoachMessageContent.AdviceSummary -> advice.toPromptText()

    // Transient UI placeholder: never part of the request history, so it renders
    // to nothing if it ever leaks here.
    CoachMessageContent.Loading -> ""
}

private fun RecommendedAction.toPromptText(): String = buildList {
    add(actionName)
    add("${sets} sets")
    reps?.let { add("×$it") }
    weightKg?.let { add("@${formatNumber(it)}kg") }
    durationSec?.let { add("${it}s") }
}.joinToString(" ")

private fun Advice.toPromptText(): String {
    val head = when (kind) {
        AdviceKind.CONTINUE_CURRENT -> "Continue ${actionName ?: "the current action"}"
        AdviceKind.SWITCH_ACTION -> "Switch to ${actionName ?: "? (another action for this part)"}"
        AdviceKind.FINISH_PART -> "This part has had enough today"
        AdviceKind.FINISH_DAY -> "Today's total volume is enough; recommend stopping"
    }
    val params = buildList {
        if (sets > 0) add("$sets sets")
        reps?.let { add("×$it") }
        weightKg?.let { add("@${formatNumber(it)}kg") }
        durationSec?.let { add("${it}s per set") }
        nextPartName?.let { add("suggested next part: $it") }
    }.joinToString(" ")
    val reason = reason?.let { "; reason: $it" }.orEmpty()
    return listOf(head, params).filter { it.isNotBlank() }.joinToString(" ") + reason
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
