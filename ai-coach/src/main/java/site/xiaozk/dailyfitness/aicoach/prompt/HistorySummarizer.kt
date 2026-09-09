package site.xiaozk.dailyfitness.aicoach.prompt

import kotlinx.datetime.LocalDate
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListActionPair

/**
 * Typed, privacy-filtered summary of a training session used for prompting.
 * Only names and numbers leave the device - no notes, no body data.
 */
data class SessionSummary(
    val date: LocalDate,
    val daysAgo: Int,
    val parts: List<PartSummary>,
)

data class PartSummary(
    val partName: String,
    val actions: List<ActionSummary>,
)

data class ActionSummary(
    val actionName: String,
    val isWeightedAction: Boolean,
    val isCountedAction: Boolean,
    val isTimedAction: Boolean,
    val sets: List<SetSummary>,
    /** true when [sets] was truncated to [MAX_SETS_PER_ACTION]. */
    val truncated: Boolean,
)

data class SetSummary(
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
)

/** Above this many sets per action only the first ones are kept (with a +n marker). */
const val MAX_SETS_PER_ACTION = 10

object HistorySummarizer {

    /**
     * Builds the summary of one [workout] day (its [DailyWorkoutListActionPair] entries are
     * grouped by part name in encounter order).
     */
    fun summarize(workout: DailyWorkout, today: LocalDate): SessionSummary? {
        if (workout.actions.isEmpty()) return null
        val daysAgo = (today.toEpochDays() - workout.date.toEpochDays()).toInt()
        val parts = workout.actions.groupBy { it.action.part.partName }
            .map { (partName, pairs) ->
                PartSummary(
                    partName = partName,
                    actions = pairs.map { pair -> summarizeAction(pair) },
                )
            }
        return SessionSummary(date = workout.date, daysAgo = daysAgo, parts = parts)
    }

    /** Keeps only the parts whose [partName] matches [partName], for "same part" history. */
    fun summarizePartOnly(workout: DailyWorkout, partName: String, today: LocalDate): SessionSummary? {
        val full = summarize(workout, today) ?: return null
        val filtered = full.parts.filter { it.partName == partName }
        if (filtered.isEmpty()) return null
        return full.copy(parts = filtered)
    }

    fun containsPart(workout: DailyWorkout, partName: String): Boolean =
        workout.actions.any { it.action.part.partName == partName }

    private fun summarizeAction(pair: DailyWorkoutListActionPair): ActionSummary {
        val action = pair.action.action
        val sets = pair.trainAction.map { set ->
            SetSummary(
                weightKg = set.takenWeight?.uniform()?.weight?.takeIf { it > 0f }?.toDouble(),
                reps = set.takenCount.takeIf { it > 0 },
                durationSec = set.takenDuration?.uniform()?.duration?.takeIf { it > 0f }?.toInt(),
            )
        }
        val truncated = sets.size > MAX_SETS_PER_ACTION
        return ActionSummary(
            actionName = pair.action.actionName,
            isWeightedAction = action.isWeightedAction,
            isCountedAction = action.isCountedAction,
            isTimedAction = action.isTimedAction,
            sets = if (truncated) sets.take(MAX_SETS_PER_ACTION) else sets,
            truncated = truncated,
        )
    }
}
