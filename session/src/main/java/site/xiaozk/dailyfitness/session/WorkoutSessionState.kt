package site.xiaozk.dailyfitness.session

import kotlin.time.Instant

/**
 * Snapshot of the current workout session, derived from persisted session meta
 * plus today's workout records (see [deriveSessionState]).
 *
 * All display data (current action, set count) comes from the repository flow;
 * the database is the single source of truth.
 */
data class WorkoutSessionState(
    val active: Boolean = false,
    val startedAt: Instant? = null,
    val currentActionId: Int? = null,
    val currentActionName: String? = null,
    /** Number of sets recorded today for the current action. */
    val setsDone: Int = 0,
    /** Total sets recorded today across all actions. */
    val totalSetsToday: Int = 0,
)
