package site.xiaozk.dailyfitness.aicoach.ui

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.engine.AiCoachResult
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.aicoach.engine.IAiCoach
import site.xiaozk.dailyfitness.repository.IAiCoachConfigStore
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import javax.inject.Inject

/**
 * FlowRedux 2.x state machine factory for the AI Coach tab.
 *
 * The machine instance is **stateless**: everything needed across transitions
 * (including the in-memory conversation) travels inside [AiCoachUiState].
 *
 * Transitions:
 * - [AiCoachUiState.Initial] probes the config -> ConfigMissing or Idle
 * - ConfigMissing: [AiCoachUiAction.SaveConfig] persists the key/model -> Idle
 * - Idle / Error: [AiCoachUiAction.Refresh] -> Loading (history carried over)
 * - Loading: on enter, runs [IAiCoach.recommendToday] with the state's history and
 *   appends the returned turn(s) into the next Idle/Error state
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiCoachStateMachine @Inject constructor(
    private val aiCoach: IAiCoach,
    private val configProvider: AiCoachConfigProvider,
    private val configStore: IAiCoachConfigStore,
) : FlowReduxStateMachineFactory<AiCoachUiState, AiCoachUiAction>() {

    init {
        initializeWith(reuseLastEmittedStateOnLaunch = false) { AiCoachUiState.Initial }
        spec {
            inState<AiCoachUiState.Initial> {
                onEnter {
                    // Wait for the first real DataStore emission before deciding.
                    val config = configProvider.config.first()
                    override {
                        if (config.configured) {
                            AiCoachUiState.Idle(setsToday = 0, content = null)
                        } else {
                            AiCoachUiState.ConfigMissing
                        }
                    }
                }
            }

            inState<AiCoachUiState.ConfigMissing> {
                on<AiCoachUiAction.SaveConfig> { action ->
                    configStore.save(AiCoachConfig(apiKey = action.apiKey.trim(), model = action.model))
                    override {
                        if (action.apiKey.isBlank()) {
                            AiCoachUiState.ConfigMissing
                        } else {
                            AiCoachUiState.Idle(setsToday = 0, content = null)
                        }
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
                    val history = snapshot.history
                    val next = runCatching { aiCoach.recommendToday(history) }
                        .getOrElse { AiCoachResult.Failed(it.message ?: "unknown", retryable = true) }
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
                            history = history.appendTurn(next.newMessages),
                        )
                        is AiCoachResult.NextAdvice -> AiCoachUiState.Idle(
                            setsToday = setsToday,
                            content = UiContent.NextAdvice(
                                advice = next.advice,
                                ignoredNames = next.ignoredNames,
                            ),
                            history = history.appendTurn(next.newMessages),
                        )
                        is AiCoachResult.Failed -> AiCoachUiState.Error(
                            setsToday = setsToday,
                            message = next.message,
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

/** 5 rounds = 10 messages kept in the UI state (and sent to the LLM). */
private const val HISTORY_MESSAGES = 10

private fun List<CoachMessage>.appendTurn(newMessages: List<CoachMessage>): List<CoachMessage> =
    if (newMessages.isEmpty()) this else (this + newMessages).takeLast(HISTORY_MESSAGES)
