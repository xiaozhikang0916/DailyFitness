package site.xiaozk.dailyfitness.aicoach.ui

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.engine.AiCoachResult
import site.xiaozk.dailyfitness.aicoach.engine.CoachFailure
import site.xiaozk.dailyfitness.aicoach.engine.IAiCoach
import javax.inject.Inject

/**
 * FlowRedux 2.x state machine factory for the AI Coach tab.
 *
 * The machine instance is **stateless**: everything needed across transitions
 * (including the in-memory conversation) travels inside [AiCoachUiState].
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
 * - a single config observer registered on the [AiCoachUiState] upper bound runs in
 *   every state: it routes `Initial` -> Ready/ConfigMissing, `ConfigMissing` -> Ready
 *   once a key is stored, and any settled state -> ConfigMissing once the key is
 *   removed
 * - Ready && !isLoading: [AiCoachUiAction.Refresh] starts a turn (pending user +
 *   loading assistant)
 * - Ready && isLoading: `onEnter` runs [IAiCoach.recommendToday] and commits the
 *   reply or a failure; [AiCoachUiAction.Cancel] commits a cancelled failure
 *   immediately (and thereby cancels the request)
 * - NoTrainParts: [AiCoachUiAction.Refresh] starts a fresh turn once a library exists
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiCoachStateMachine @Inject constructor(
    private val aiCoach: IAiCoach,
    private val configProvider: AiCoachConfigProvider,
) : FlowReduxStateMachineFactory<AiCoachUiState, AiCoachUiAction>() {

    init {
        initializeWith(reuseLastEmittedStateOnLaunch = false) { AiCoachUiState.Initial }
        spec {
            // Continuously observe the config in every state (registered on the
            // sealed-interface upper bound). This is the single place that reconciles
            // the screen with the config: Initial routes to Ready/ConfigMissing,
            // ConfigMissing -> Ready once a key appears, and any settled state ->
            // ConfigMissing once the key is removed.
            inState<AiCoachUiState> {
                collectWhileInState(configProvider.config) { config ->
                    when {
                        config.configured &&
                            (snapshot is AiCoachUiState.Initial ||
                                snapshot is AiCoachUiState.ConfigMissing) ->
                            override { AiCoachUiState.Ready(setsToday = 0) }
                        !config.configured && snapshot !is AiCoachUiState.ConfigMissing ->
                            override { AiCoachUiState.ConfigMissing }
                        else -> noChange()
                    }
                }
            }

            inState<AiCoachUiState.Ready> {
                // Independent of the request lifecycle: works while loading too. The
                // resulting Ready -> Ready update keeps `isLoading` unchanged, so the
                // in-flight request is not cancelled.
                on<AiCoachUiAction.TodayInfo> { action ->
                    mutate { copy(setsToday = action.setsToday) }
                }

                condition({ !it.isLoading }, name = "ready-idle") {
                    on<AiCoachUiAction.Refresh> { action ->
                        override { startTurn(action.userContent) }
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
                // No TodayInfo handler: the library is empty, so setsToday is fixed at 0.
                // Retry after the user built a library: a fresh turn moves the machine
                // into Ready, whose loading condition runs the request.
                on<AiCoachUiAction.Refresh> { action ->
                    override {
                        AiCoachUiState.Ready(setsToday = 0).startTurn(action.userContent)
                    }
                }
            }
        }
    }
}
