package site.xiaozk.dailyfitness.aicoach.ui

import androidx.compose.runtime.Immutable
import site.xiaozk.dailyfitness.aicoach.engine.CoachFailure
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessageContent

/**
 * FlowRedux state of the AI Coach tab.
 *
 * The conversation *is* the state: a pending ("AI is thinking") turn and a failed /
 * aborted turn are ordinary [CoachMessage]s in [Ready.history]. Whether the screen
 * is loading is therefore a derived property of the last message rather than a
 * separate state class, and `history` has one uniform meaning in every state.
 *
 * The state machine keys its request / abort side effects off [Ready.isLoading], so
 * a request runs only while the last turn is a pending one and is cancelled by
 * FlowRedux as soon as that predicate flips.
 */
@Immutable
sealed interface AiCoachUiState {
    /** How many sets were recorded today; carried by every state for the header. */
    val setsToday: Int

    /** Rendered conversation, oldest first: model-backed turns plus UI-only ones. */
    val history: List<CoachMessage>

    /** Before the machine has settled (initial config probe). */
    data object Initial : AiCoachUiState {
        override val setsToday: Int = 0
        override val history: List<CoachMessage> = emptyList()
    }

    /** AI not configured; the tab points the user to the settings page. */
    data object ConfigMissing : AiCoachUiState {
        override val setsToday: Int = 0
        override val history: List<CoachMessage> = emptyList()
    }

    /**
     * No train parts/actions exist yet, so nothing can be recommended; the tab points
     * the user to build the library first. A pure page gate like [ConfigMissing]: no
     * conversation is worth keeping and no set could have been recorded today.
     */
    data object NoTrainParts : AiCoachUiState {
        override val setsToday: Int = 0
        override val history: List<CoachMessage> = emptyList()
    }

    /**
     * Settled screen. No request side effect runs unless the last turn is a pending
     * one, which the machine expresses through [isLoading].
     *
     * The two lists have distinct jobs and the turn helpers below are the only place
     * they are written together:
     * - [history] is what the chat renders (successful + failed + the pending turn);
     * - [requestHistory] is what the model sees (successful turns only).
     */
    @Immutable
    data class Ready(
        override val setsToday: Int,
        override val history: List<CoachMessage> = emptyList(),
        /** Committed model conversation; a failed/aborted turn never lands here. */
        val requestHistory: List<CoachMessage> = emptyList(),
    ) : AiCoachUiState {
        /**
         * True while the last turn is the transient "AI is thinking" bubble. Pure
         * function of the immutable state - the machine uses it as a `condition`.
         */
        val isLoading: Boolean get() = history.lastOrNull()?.isLoading == true

        /**
         * Starts a turn: appends the locally-built user bubble and its pending
         * assistant bubble, sharing one turn id. Display only - [requestHistory] is
         * untouched until the turn succeeds.
         */
        fun startTurn(userContent: CoachMessageContent): Ready {
            val turnId = history.count { it.fromUser }
            return copy(
                history = history +
                    CoachMessage(fromUser = true, content = userContent, turnId = turnId) +
                    CoachMessage(fromUser = false, content = CoachMessageContent.Loading, turnId = turnId),
            )
        }

        /**
         * Commits a successful turn: the reply replaces the loading bubble and joins
         * the model conversation. The single place both lists are written.
         */
        fun commitReply(reply: CoachMessage): Ready {
            // startTurn always appends the user bubble then the loading assistant bubble.
            val pendingUser = history[history.size - 2]
            val committedReply = reply.copy(turnId = pendingUser.turnId)
            return copy(
                history = history.dropLast(1) + committedReply,
                requestHistory = requestHistory + pendingUser + committedReply,
            )
        }

        /** Commits a failed/aborted turn: visible to the user, never sent to the model. */
        fun commitFailure(failure: CoachFailure): Ready {
            val loading = history.last()
            return copy(
                history = history.dropLast(1) + loading.copy(content = CoachMessageContent.Failure(failure)),
            )
        }
    }
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

    /** Abort the in-flight request; the machine turns the pending turn into a failure. */
    data object Cancel : AiCoachUiAction
}
