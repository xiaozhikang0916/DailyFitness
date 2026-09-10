package site.xiaozk.dailyfitness.aicoach.ui

import site.xiaozk.dailyfitness.aicoach.engine.Advice
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.aicoach.engine.RecommendedPart
import site.xiaozk.dailyfitness.repository.model.AiCoachModel

/** FlowRedux state of the AI Coach tab. */
sealed interface AiCoachUiState {
    /** Before the machine has settled (initial config/history probe). */
    data object Initial : AiCoachUiState

    /** AI not configured; the tab shows the config form. */
    data object ConfigMissing : AiCoachUiState

    /** Stable: last fetch content (if any) + how many sets recorded today. */
    data class Idle(
        val setsToday: Int,
        val content: UiContent?,
    ) : AiCoachUiState

    /** A recommendation request is running. */
    data class Loading(
        val setsToday: Int,
    ) : AiCoachUiState

    data class Error(
        val setsToday: Int,
        val message: String,
        val retryable: Boolean,
    ) : AiCoachUiState
}

sealed interface UiContent {
    data object NoTrainParts : UiContent

    data class TodayPlan(
        val parts: List<RecommendedPart>,
        val sessionsUsed: Int,
        val rounds: Int,
        val ignoredNames: List<String>,
        val newMessages: List<CoachMessage> = emptyList(),
    ) : UiContent

    data class NextAdvice(
        val advice: Advice,
        val ignoredNames: List<String>,
        val newMessages: List<CoachMessage> = emptyList(),
    ) : UiContent
}

/** Actions dispatched into the FlowRedux machine. */
sealed interface AiCoachUiAction {
    /** Fetch/re-fetch the recommendation; carries the session-memory conversation. */
    data class Refresh(val history: List<CoachMessage> = emptyList()) : AiCoachUiAction

    /** Live update of how many sets were recorded today (fed by the VM). */
    data class TodayInfo(val setsToday: Int) : AiCoachUiAction

    /** Persist a new AI config from the (v1 transitional) config form. */
    data class SaveConfig(
        val apiKey: String,
        val model: AiCoachModel,
    ) : AiCoachUiAction
}
