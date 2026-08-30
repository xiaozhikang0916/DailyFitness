package site.xiaozk.dailyfitness.session

import site.xiaozk.dailyfitness.repository.model.DailyWorkout

/**
 * Pure derivation function: combines persisted session meta with today's
 * workout data into a displayable [WorkoutSessionState].
 *
 * v1 policy: the "current action" auto-follows the most recently recorded set
 * of the day; [WorkoutSessionState.setsDone] counts today's sets of that action.
 */
fun deriveSessionState(meta: SessionMeta, workout: DailyWorkout?): WorkoutSessionState {
    if (!meta.active) {
        return WorkoutSessionState()
    }
    if (workout == null || workout.actions.isEmpty()) {
        return WorkoutSessionState(active = true, startedAt = meta.startedAt)
    }
    val latestPair = workout.actions.maxBy { pair ->
        pair.trainAction.maxOf { it.instant }
    }
    val currentAction = latestPair.action
    return WorkoutSessionState(
        active = true,
        startedAt = meta.startedAt,
        currentActionId = currentAction.id,
        currentActionName = currentAction.actionName,
        setsDone = latestPair.trainAction.size,
        totalSetsToday = workout.actions.sumOf { it.trainAction.size },
    )
}
