package site.xiaozk.dailyfitness.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [WorkoutSessionController] implementation.
 *
 * - state is derived from [SessionStore] (persisted meta) combined with today's
 *   workout flow from the repository
 * - [start] / [finish] only touch the persisted meta; the repository flow
 *   reactively updates the derived state (set counts, current action)
 *
 * The foreground service / notification wiring is layered on top in later
 * milestones, this class itself has no Android UI dependency.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class WorkoutSessionControllerImpl @Inject constructor(
    private val store: SessionStore,
    private val workoutRepo: IDailyWorkoutRepository,
    private val userRepo: IUserRepository,
) : WorkoutSessionController {

    /**
     * The session state must stay alive as long as the application process
     * (the notification service rebuilds it after process death), so the
     * controller owns its own scope instead of injecting one into the
     * singleton component. Overridable from tests in this module.
     */
    internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val state: StateFlow<WorkoutSessionState> by lazy {
        combine(
            store.observe(),
            flow { emit(userRepo.getCurrentUser()) }
                .flatMapLatest { user -> workoutRepo.getWorkoutOfDayFlow(user, today()) },
        ) { meta, workout ->
            deriveSessionState(meta, workout)
        }.stateIn(scope, SharingStarted.Eagerly, WorkoutSessionState())
    }

    override suspend fun start() {
        if (state.value.active) return
        store.setActive(SessionMeta(active = true, startedAt = Clock.System.now()))
    }

    override suspend fun finish() {
        store.setActive(SessionMeta())
    }
}

private fun today(): LocalDate =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
