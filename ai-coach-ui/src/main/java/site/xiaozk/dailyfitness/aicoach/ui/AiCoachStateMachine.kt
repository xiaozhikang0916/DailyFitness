package site.xiaozk.dailyfitness.aicoach.ui

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Transitions:
 * - a single config observer registered on the [AiCoachUiState] upper bound runs in
 *   every state: it routes `Initial` -> Idle/ConfigMissing, `ConfigMissing` -> Idle
 *   once a key is stored, and any settled state -> ConfigMissing once the key is
 *   removed
 * - Idle / Error: [AiCoachUiAction.Refresh] -> Loading (history carried over)
 * - Loading: on enter, runs [IAiCoach.recommendToday] with the state's history and
 *   appends the returned turn(s) into the next Idle/Error state
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
            // the screen with the config: Initial routes to Idle/ConfigMissing,
            // ConfigMissing -> Idle once a key appears, and any settled state ->
            // ConfigMissing once the key is removed.
            inState<AiCoachUiState> {
                collectWhileInState(configProvider.config) { config ->
                    when {
                        config.configured &&
                            (snapshot is AiCoachUiState.Initial ||
                                snapshot is AiCoachUiState.ConfigMissing) ->
                            override { AiCoachUiState.Idle(setsToday = 0, content = null) }
                        !config.configured && snapshot !is AiCoachUiState.ConfigMissing ->
                            override { AiCoachUiState.ConfigMissing }
                        else -> noChange()
                    }
                }
            }

            inState<AiCoachUiState.Idle> {
                on<AiCoachUiAction.TodayInfo> { action ->
                    mutate { copy(setsToday = action.setsToday) }
                }
                on<AiCoachUiAction.Refresh> {
                    override {
                        AiCoachUiState.Loading(setsToday = this.setsToday, history = this.history)
                    }
                }
            }

            inState<AiCoachUiState.Error> {
                on<AiCoachUiAction.TodayInfo> { action ->
                    mutate { copy(setsToday = action.setsToday) }
                }
                on<AiCoachUiAction.Refresh> {
                    override {
                        AiCoachUiState.Loading(setsToday = this.setsToday, history = this.history)
                    }
                }
            }

            inState<AiCoachUiState.Loading> {
                onEnter {
                    val setsToday = snapshot.setsToday
                    // Full in-memory conversation; the 5-round/10-message request cap lives in AiCoachEngine.
                    val history = snapshot.history
                    val next = runCatching { aiCoach.recommendToday(history) }
                        .getOrElse {
                            AiCoachResult.Failed(
                                CoachFailure.ModelError(it.message ?: it.javaClass.simpleName),
                                retryable = true,
                            )
                        }
                    val target = when (next) {
                        is AiCoachResult.ConfigMissing -> AiCoachUiState.ConfigMissing
                        AiCoachResult.NoTrainParts ->
                            AiCoachUiState.Idle(setsToday, UiContent.NoTrainParts, history)
                        is AiCoachResult.TodayPlan -> AiCoachUiState.Idle(
                            setsToday = setsToday,
                            content = UiContent.TodayPlan(
                                parts = next.parts,
                                sessionsUsed = next.sessionsUsed,
                                rounds = next.rounds,
                                ignoredNames = next.ignoredNames,
                            ),
                            history = history + next.newMessages,
                        )
                        is AiCoachResult.NextAdvice -> AiCoachUiState.Idle(
                            setsToday = setsToday,
                            content = UiContent.NextAdvice(
                                advice = next.advice,
                                ignoredNames = next.ignoredNames,
                                suggestions = next.newMessages.flatMap { it.suggestions },
                            ),
                            history = history + next.newMessages,
                        )
                        is AiCoachResult.Failed -> AiCoachUiState.Error(
                            setsToday = setsToday,
                            failure = next.failure,
                            retryable = next.retryable,
                            history = history,
                        )
                    }
                    override { target }
                }
            }
        }
    }

}
