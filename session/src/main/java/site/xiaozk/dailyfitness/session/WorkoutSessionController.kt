package site.xiaozk.dailyfitness.session

import kotlinx.coroutines.flow.StateFlow

/**
 * Facade of the workout session feature, owned by the [:session] module.
 *
 * The [app] module only wires entry points (start button, notification
 * navigation) through this interface - it never touches internal state.
 */
interface WorkoutSessionController {

    /**
     * Observable session state. Starts deriving once the application process
     * is up, so the notification can be rebuilt after process death.
     */
    val state: StateFlow<WorkoutSessionState>

    /** Starts a workout session (persists [SessionMeta] and turns the state active). */
    suspend fun start()

    /** Finishes the session: clears persisted meta, state turns inactive. */
    suspend fun finish()
}
