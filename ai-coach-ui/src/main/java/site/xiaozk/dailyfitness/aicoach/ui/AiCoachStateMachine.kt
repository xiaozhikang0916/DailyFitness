package site.xiaozk.dailyfitness.aicoach.ui

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.engine.AiCoachResult
import site.xiaozk.dailyfitness.aicoach.engine.IAiCoach
import site.xiaozk.dailyfitness.repository.IAiCoachConfigStore
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import javax.inject.Inject

/**
 * FlowRedux 2.x state machine factory for the AI Coach tab.
 *
 * Transitions:
 * - [AiCoachUiState.Initial] probes the config -> ConfigMissing or Idle
 * - ConfigMissing: [AiCoachUiAction.SaveConfig] persists the key/model -> Idle
 * - Idle / Error: [AiCoachUiAction.Refresh] -> Loading
 * - Loading: on enter, runs [IAiCoach.recommendToday] -> Idle(content) / Error / ConfigMissing
 * - Stationary states keep [AiCoachUiState.setsToday] in sync via
 *   [AiCoachUiAction.TodayInfo] (dispatched by the ViewModel from the DB flow).
 *
 * The factory is launched by the ViewModel in its viewModelScope.
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
                    override { AiCoachUiState.Loading(setsToday = this.setsToday) }
                }
            }

            inState<AiCoachUiState.Error> {
                on<AiCoachUiAction.TodayInfo> { action ->
                    mutate { copy(setsToday = action.setsToday) }
                }
                on<AiCoachUiAction.Refresh> {
                    override { AiCoachUiState.Loading(setsToday = this.setsToday) }
                }
            }

            inState<AiCoachUiState.Loading> {
                onEnter {
                    val setsToday = snapshot.setsToday
                    val next = runCatching { aiCoach.recommendToday() }
                        .getOrElse { AiCoachResult.Failed(it.message ?: "unknown", retryable = true) }
                    val target = when (next) {
                        is AiCoachResult.ConfigMissing -> AiCoachUiState.ConfigMissing
                        AiCoachResult.NoTrainParts ->
                            AiCoachUiState.Idle(setsToday, UiContent.NoTrainParts)
                        is AiCoachResult.TodayPlan -> AiCoachUiState.Idle(
                            setsToday,
                            UiContent.TodayPlan(
                                parts = next.parts,
                                sessionsUsed = next.sessionsUsed,
                                rounds = next.rounds,
                                ignoredNames = next.ignoredNames,
                            ),
                        )
                        is AiCoachResult.NextAdvice -> AiCoachUiState.Idle(
                            setsToday,
                            UiContent.NextAdvice(advice = next.advice, ignoredNames = next.ignoredNames),
                        )
                        is AiCoachResult.Failed -> AiCoachUiState.Error(
                            setsToday = setsToday,
                            message = next.message,
                            retryable = next.retryable,
                        )
                    }
                    override { target }
                }
            }
        }
    }
}
