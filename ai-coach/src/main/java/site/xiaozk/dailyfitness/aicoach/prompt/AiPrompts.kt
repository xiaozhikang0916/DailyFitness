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
You are the user's personal fitness coach.
Your task: based on the [Training History] and the [Exercise Catalog], and referring to the user's previous part-rotation habits and the intervals between sessions, recommend the body parts to train today and give a complete plan the user can follow directly.

Hard rules:
1. Part names and action names must be copied verbatim from the [Exercise Catalog]; inventing, abbreviating, or using similar names is forbidden.
2. Only recommend parts and actions that actually exist in the [Exercise Catalog].
3. Units: weight is always kg, reps are integers, duration is integer seconds.
4. An action's "weighted/counted/timed" markers decide which fields may be filled: weighted -> weightKg; counted -> reps; timed -> durationSec. Do not fill fields the action does not support.
5. Based on the number of sets the user habitually trains, give the recommended number of sets (sets); lower it appropriately for a first session or after a long break.
6. Normally recommend 1-2 parts; the primary part has isPrimary=true and the other one is an alternative; give 1-4 actions per part.
7. If you judge the history to be insufficient for a reliable recommendation: set needMore=true and give wantSessions=how many more complete training days are needed; do not fill plan in this case.
8. If the history is sufficient (or you have been explicitly told that only this history can be provided): needMore=false, and plan must be filled.
9. reason: explain the recommendation in one or two short sentences.
""".trimIndent()

    private val NEXT_ADVICE_SYSTEM = """
You are the user's personal fitness coach. The user is training and you decide what to do next.
The input contains: [Today's Completed Sets], the [Recent History for This Part], and the [Exercise Catalog].
Base your recommendation on the user's habits with this action - whether they start light and work up, how many sets they usually do, and whether today's session so far can handle a higher intensity - and then recommend the next action.
Choose exactly one of the following four actions:
- continue_current: continue the current action with another set -> fill actionName (the current action) + sets (how many more sets to add) + weightKg/reps or durationSec (per action type, adjusted from the previous set);
- switch_action: the current action has enough sets; switch to another action for the same part -> actionName is the new action name + the new action's recommended sets/reps/weightKg/durationSec;
- finish_part: this part has had enough volume today -> optionally fill nextPartName with the part to train next (must come from the catalog; leave it empty if unsure);
- finish_day: today's total volume is enough; recommend ending the workout.

Hard rules:
1. All part names/action names must come verbatim from the [Exercise Catalog].
2. Units: weight in kg, reps as integers, duration as integer seconds; only fill the fields allowed by the action type.
3. reason: explain in one or two sentences (e.g. training volume, days since the last session, progressive overload).
""".trimIndent()

    /**
     * Language contract appended to every system prompt.
     *
     * Free-text fields follow the caller's locale, while names taken from the
     * catalog must stay verbatim (they are user data, not model copy).
     */
    private fun languageInstruction(localeTag: String): String = """
Output language:
- The user's locale is "$localeTag". Write every free-text field (such as reason) in the language matching that locale.
- Part names and action names copied from the [Exercise Catalog] must keep their original wording and must never be translated.
""".trimIndent()

    fun partPlanSystem(localeTag: String): String =
        PART_PLAN_SYSTEM + "\n\n" + languageInstruction(localeTag)

    fun nextAdviceSystem(localeTag: String): String =
        NEXT_ADVICE_SYSTEM + "\n\n" + languageInstruction(localeTag)

    /** [Exercise Catalog] section text, names only (+ type markers). Groups come straight from the repository models. */
    fun formatCatalog(groups: List<TrainPartGroup>): String = buildString {
        appendLine("[Exercise Catalog] (choose names only from here)")
        groups.forEach { group ->
            val actions = group.actions.joinToString(", ") { action ->
                action.actionName + actionTypeMark(action.isWeightedAction, action.isCountedAction, action.isTimedAction)
            }
            appendLine("- ${group.part.partName}: $actions")
        }
    }.trimEnd()

    /** One session block, e.g. "3 days ago (2025-01-05)" + parts/actions/sets. */
    fun formatSessionDay(session: SessionSummary, orderLabel: String? = null): String = buildString {
        val relative = if (session.daysAgo == 0) "today" else "${session.daysAgo} days ago"
        val prefix = if (orderLabel.isNullOrBlank()) "" else "$orderLabel "
        appendLine("$prefix$relative (${session.date})")
        session.parts.forEach { part ->
            appendLine("  Part[${part.partName}]")
            part.actions.forEach { action ->
                appendLine("    - ${action.actionName}${actionTypeMark(action)}")
                val suffix = if (action.truncated) " (more than ${action.sets.size} sets total; showing first $MAX_SETS_PER_ACTION)" else ""
                action.sets.forEach { set ->
                    appendLine("      ${formatSet(action, set)}")
                }
                if (suffix.isNotEmpty()) appendLine("      $suffix")
            }
        }
    }

    fun formatHistory(sessions: List<SessionSummary>): String = buildString {
        appendLine("[Training History] ${sessions.size} training day(s)")
        sessions.forEachIndexed { index, session ->
            append(formatSessionDay(session, orderLabel = "Session ${sessions.size - index}"))
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
            appendLine("[Training History] empty: there are no past sessions to reference. Give a reasonable first-time recommendation based directly on the [Exercise Catalog];")
            appendLine("do not set needMore (there is no more history to provide) and you must return a complete plan.")
        } else {
            appendLine(formatHistory(history))
        }
        if (!additionalNote.isNullOrBlank()) {
            appendLine()
            appendLine("[Additional Note] $additionalNote")
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
        appendLine("[Today's Completed Sets]")
        appendLine(formatSessionDay(todaySession))
        appendLine()
        if (partHistory.isEmpty()) {
            if (lastPartDaysAgo == null) {
                appendLine("[Recent History for This Part] empty: this part has never been trained before today; base your judgment mainly on today's completed sets and the catalog.")
            } else {
                appendLine("[Recent History for This Part] empty (this part was last trained $lastPartDaysAgo days ago, but that session is outside the history window below).")
            }
        } else {
            appendLine("[Recent History for This Part] last trained this part: $lastPartDaysAgo days ago")
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
            parts += formatNumber(set.durationSec.toDouble()) + "s"
        }
        if (parts.isEmpty()) parts += "(no recorded parameters)"
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
            if (weighted) add("weighted")
            if (counted) add("counted")
            if (timed) add("timed")
        }
        return if (marks.isEmpty()) "(no parameters)" else "(${marks.joinToString("+")})"
    }
}
