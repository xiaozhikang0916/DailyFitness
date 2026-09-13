package site.xiaozk.dailyfitness.aicoach.ui

import site.xiaozk.dailyfitness.aicoach.engine.CoachFailure
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessageContent

/**
 * FlowRedux state of the AI Coach tab.
 *
 * Every piece of state (including the in-memory conversation [CoachMessage] list
 * used for multi-turn requests and chat rendering) lives here - the state machine
 * instance itself is stateless.
 */
sealed interface AiCoachUiState {
    /**
     * Conversation carried by the state, for chat rendering and next requests.
     *
     * Each state exposes it directly so a state can shape the rendered list without
     * the UI knowing about it: [Loading] appends the pending user turn and a pending
     * assistant (loading) bubble.
     */
    val history: List<CoachMessage>

    /** Before the machine has settled (initial config/history probe). */
    data object Initial : AiCoachUiState {
        override val history: List<CoachMessage> = emptyList()
    }

    /** AI not configured; the tab points the user to the settings page. */
    data object ConfigMissing : AiCoachUiState {
        override val history: List<CoachMessage> = emptyList()
    }

    /** Stable: last fetch content (if any) + how many sets recorded today. */
    data class Idle(
        val setsToday: Int,
        val content: UiContent?,
        /** Full in-memory session memory, newest last (request side trims to 5 rounds). */
        override val history: List<CoachMessage> = emptyList(),
    ) : AiCoachUiState

    /** A recommendation request is running. */
    data class Loading(
        val setsToday: Int,
        /** Raw conversation handed to the engine; excludes the pending turn. */
        val requestHistory: List<CoachMessage> = emptyList(),
        /** Locally-built user turn descriptor for this request. */
        val pendingUserContent: CoachMessageContent,
    ) : AiCoachUiState {
        /**
         * Identity of the turn being requested. Derived from the history length so
         * it is unique per appended turn without extra state.
         */
        val pendingTurnId: String = "turn-${requestHistory.size}"

        // Constructed once per Loading instance so their identity (incl. `at`/`id`) is
        // stable across recompositions, which keeps the bubbles recomposing in place.
        private val pendingUser = CoachMessage(
            fromUser = true,
            content = pendingUserContent,
            id = "$pendingTurnId:user",
        )
        private val pendingAssistant = CoachMessage(
            fromUser = false,
            content = CoachMessageContent.Loading,
            id = "$pendingTurnId:assistant",
        )

        /** Rendered conversation: request history + pending user bubble + loading bubble. */
        override val history: List<CoachMessage> get() = requestHistory + pendingUser + pendingAssistant
    }

    data class Error(
        val setsToday: Int,
        val failure: CoachFailure,
        val retryable: Boolean,
        override val history: List<CoachMessage> = emptyList(),
    ) : AiCoachUiState
}

sealed interface UiContent {
    data object NoTrainParts : UiContent
}

/** Actions dispatched into the FlowRedux machine. */
sealed interface AiCoachUiAction {
    /**
     * Fetch/re-fetch the recommendation. [userContent] is the locally-built user turn,
     * shown immediately so the user bubble precedes the loading bubble.
     */
    data class Refresh(val userContent: CoachMessageContent) : AiCoachUiAction

    /** Live update of how many sets were recorded today (fed by the VM). */
    data class TodayInfo(val setsToday: Int) : AiCoachUiAction
}
