package site.xiaozk.dailyfitness.aicoach.ui

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.engine.AiCoachResult
import site.xiaozk.dailyfitness.aicoach.engine.CoachFailure
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessageContent
import site.xiaozk.dailyfitness.aicoach.engine.IAiCoach
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import javax.inject.Inject

/**
 * FlowRedux 2.x state machine factory for the AI Coach tab.
 *
 * The machine instance is **stateless**: everything needed across transitions
 * (including the in-memory conversation and today's training state) travels inside
 * [AiCoachUiState]. The ViewModel only forwards UI actions.
 *
 * The conversation is modelled as messages, so "loading" and "failed" are just the
 * content of the last turn. The machine keys its side effects off that with
 * `condition`, which FlowRedux folds into the side effect's `isInState`: flipping
 * [AiCoachUiState.Ready.isLoading] cancels the running request job, which is how
 * both completion and [AiCoachUiAction.Cancel] interrupt it. All turn mutation goes
 * through the [AiCoachUiState.Ready] helpers, so the rendered and model
 * conversations cannot drift.
 *
 * Transitions:
 * - a single observer registered on the [AiCoachUiState] upper bound reconciles the
 *   config gate and today's training: it routes `Initial` -> Ready/ConfigMissing,
 *   ConfigMissing -> Ready once a key is stored, keeps Ready's sets/part fresh, and
 *   moves any settled state -> ConfigMissing once the key is removed
 * - Ready && !isLoading: [AiCoachUiAction.Refresh] builds the user turn from the
 *   observed today state and starts it (pending user + loading assistant)
 * - Ready && isLoading: `onEnter` runs [IAiCoach.recommendToday] and commits the
 *   reply or a failure; [AiCoachUiAction.Cancel] commits a cancelled failure
 *   immediately (and thereby cancels the request)
 * - NoTrainParts: [AiCoachUiAction.Refresh] starts a fresh turn once a library exists
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiCoachStateMachine @Inject constructor(
    private val aiCoach: IAiCoach,
    private val configProvider: AiCoachConfigProvider,
    private val userRepository: IUserRepository,
    private val workoutRepository: IDailyWorkoutRepository,
) : FlowReduxStateMachineFactory<AiCoachUiState, AiCoachUiAction>() {

    /**
     * Config gate + today's training, observed for the whole machine lifetime. Both
     * are combined so entering `Ready` already carries today's sets/part, instead of
     * racing a separate today emission that could land while the state is not Ready.
     */
    private val observedFlow: Flow<Observed> = combine(
        configProvider.config,
        todayTrainingFlow(),
    ) { config, today -> Observed(configured = config.configured, today = today) }
        .distinctUntilChanged()

    init {
        initializeWith(reuseLastEmittedStateOnLaunch = false) { AiCoachUiState.Initial }
        spec {
            inState<AiCoachUiState> {
                collectWhileInState(observedFlow, name = "observed") { observed ->
                    override { reconciledWith(observed) }
                }
            }

            inState<AiCoachUiState.Ready> {
                condition({ !it.isLoading }, name = "ready-idle") {
                    on<AiCoachUiAction.Refresh> {
                        // Built from the observed state, not passed in: the ViewModel is stateless.
                        val userContent = userContentFor(snapshot.setsToday, snapshot.currentPart)
                        override { startTurn(userContent) }
                    }
                }

                condition({ it.isLoading }, name = "ready-loading") {
                    // Handled before the request finishes in the common case; the state
                    // change flips `isLoading` and FlowRedux cancels the onEnter job.
                    on<AiCoachUiAction.Cancel> {
                        override { commitFailure(CoachFailure.Cancelled) }
                    }

                    onEnter {
                        // The model conversation only: pending/failed turns never reach it.
                        val next = try {
                            aiCoach.recommendToday(snapshot.requestHistory)
                        } catch (it: Throwable) {
                            // Re-throw only if this coroutine itself was cancelled (e.g.
                            // the request was aborted or the state changed); a random
                            // CancellationException from an inner call must not cancel
                            // the machine, so it is surfaced as a normal failure instead.
                            currentCoroutineContext().ensureActive()
                            AiCoachResult.Failed(
                                CoachFailure.ModelError(it.message ?: it.javaClass.simpleName),
                                retryable = true,
                            )
                        }
                        val target: AiCoachUiState = when (next) {
                            is AiCoachResult.ConfigMissing -> AiCoachUiState.ConfigMissing
                            AiCoachResult.NoTrainParts -> AiCoachUiState.NoTrainParts
                            is AiCoachResult.TodayPlan -> snapshot.commitReply(next.assistantMessage)
                            is AiCoachResult.NextAdvice -> snapshot.commitReply(next.assistantMessage)
                            is AiCoachResult.Failed -> snapshot.commitFailure(next.failure)
                        }
                        override { target }
                    }
                }
            }

            inState<AiCoachUiState.NoTrainParts> {
                // setsToday is fixed at 0: with an empty library nothing could have been
                // trained. Retry after the library is built moves into Ready, whose
                // loading condition runs the request.
                on<AiCoachUiAction.Refresh> {
                    override {
                        AiCoachUiState.Ready(setsToday = 0)
                            .startTurn(CoachMessageContent.PlanRequest(todayLocalDate()))
                    }
                }
            }
        }
    }

    /**
     * Today's training, derived from the repositories. Cold: every launched machine
     * observes its own copy. Reads are local (no network), so this stays within the
     * privacy invariant.
     */
    private fun todayTrainingFlow(): Flow<TodayTraining> = flow {
        val user = userRepository.getCurrentUser()
        val today = todayLocalDate()
        emitAll(
            workoutRepository.getWorkoutOfDayFlow(user, today)
                .map { it.toTodayTraining() },
        )
    }.distinctUntilChanged()

    private fun userContentFor(setsToday: Int, currentPart: String?): CoachMessageContent {
        val today = todayLocalDate()
        return if (setsToday <= 0) {
            CoachMessageContent.PlanRequest(today)
        } else {
            CoachMessageContent.AdviceRequest(
                date = today,
                setsToday = setsToday,
                partName = currentPart.orEmpty(),
            )
        }
    }
}

/** Config gate + today's training, the only inputs the screen reconciles against. */
internal data class Observed(val configured: Boolean, val today: TodayTraining)

/** What today's training looks like: how many sets and the most recent part. */
internal data class TodayTraining(val setsToday: Int, val currentPart: String?)

/**
 * Reconciles the observed config gate and today's training into the current state.
 *
 * Pure and exhaustive over the sealed state so adding a state forces a decision here;
 * applied via `override { reconciledWith(...) }` so it runs against the state at reduce
 * time rather than a possibly stale snapshot.
 */
internal fun AiCoachUiState.reconciledWith(observed: Observed): AiCoachUiState = when (this) {
    AiCoachUiState.Initial,
    AiCoachUiState.ConfigMissing -> if (observed.configured) {
        AiCoachUiState.Ready(
            setsToday = observed.today.setsToday,
            currentPart = observed.today.currentPart,
        )
    } else {
        AiCoachUiState.ConfigMissing
    }
    is AiCoachUiState.Ready -> if (observed.configured) {
        copy(setsToday = observed.today.setsToday, currentPart = observed.today.currentPart)
    } else {
        AiCoachUiState.ConfigMissing
    }
    AiCoachUiState.NoTrainParts -> if (observed.configured) this else AiCoachUiState.ConfigMissing
}

private fun DailyWorkout?.toTodayTraining(): TodayTraining = TodayTraining(
    setsToday = this?.actions?.sumOf { it.trainAction.size } ?: 0,
    currentPart = this?.actions
        ?.filter { it.trainAction.isNotEmpty() }
        ?.maxByOrNull { pair -> pair.trainAction.maxOf { it.instant } }
        ?.action?.part?.partName,
)

private fun todayLocalDate(): LocalDate =
    kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
