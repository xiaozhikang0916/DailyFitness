package site.xiaozk.dailyfitness.aicoach.ui

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.engine.AiCoachResult
import site.xiaozk.dailyfitness.aicoach.engine.CoachFailure
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
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
                on<AiCoachUiAction.Refresh> { action ->
                    override {
                        AiCoachUiState.Loading(
                            setsToday = this.setsToday,
                            requestHistory = this.history,
                            pendingUserContent = action.userContent,
                        )
                    }
                }
            }

            inState<AiCoachUiState.Error> {
                on<AiCoachUiAction.TodayInfo> { action ->
                    mutate { copy(setsToday = action.setsToday) }
                }
                on<AiCoachUiAction.Refresh> { action ->
                    override {
                        AiCoachUiState.Loading(
                            setsToday = this.setsToday,
                            requestHistory = this.history,
                            pendingUserContent = action.userContent,
                        )
                    }
                }
            }

            inState<AiCoachUiState.Loading> {
                onEnter {
                    val setsToday = snapshot.setsToday
                    val turnId = snapshot.pendingTurnId
                    // Raw conversation only: the pending user/loading bubbles added by
                    // Loading.history must never be sent to the engine/LLM.
                    val history = snapshot.requestHistory
                    // Rendered history minus the trailing pending loading bubble; the
                    // reply replaces it, keeping the pending user bubble in place.
                    val baseHistory = snapshot.history.dropLast(1)
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
                            content = null,
                            history = baseHistory + next.assistantMessage.taggedAsAssistant(turnId),
                        )
                        is AiCoachResult.NextAdvice -> AiCoachUiState.Idle(
                            setsToday = setsToday,
                            content = null,
                            history = baseHistory + next.assistantMessage.taggedAsAssistant(turnId),
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

/**
 * Stamps the assistant reply with the id of the pending loading bubble
 * (`<turnId>:assistant`), so the LazyColumn reuses the same item when the reply
 * arrives and the loading bubble can animate into it in place.
 */
private fun CoachMessage.taggedAsAssistant(turnId: String): CoachMessage =
    copy(id = "$turnId:assistant")
