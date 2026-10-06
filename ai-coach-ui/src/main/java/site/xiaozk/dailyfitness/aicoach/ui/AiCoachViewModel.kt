package site.xiaozk.dailyfitness.aicoach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.processor.A2uiSurfaceModel
import androidx.a2ui.model.processor.processInput
import com.freeletics.flowredux2.FlowReduxStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessageContent
import site.xiaozk.dailyfitness.aicoach.engine.CoachSuggestion
import site.xiaozk.dailyfitness.aicoach.ui.a2ui.DailyFitnessA2uiActions
import site.xiaozk.dailyfitness.aicoach.ui.a2ui.DailyFitnessA2uiCatalog
import javax.inject.Inject

/**
 * Activity-scoped tab ViewModel launching the FlowRedux [AiCoachStateMachine]
 * factory in the ViewModel scope.
 *
 * The ViewModel is **stateless**: it only forwards UI actions and exposes the
 * machine's state. Everything durable - the in-memory conversation and the observed
 * today-training state - lives in [AiCoachUiState], which is why an activity-scoped
 * VM is enough for the chat to survive bottom-tab switches.
 */
@HiltViewModel
class AiCoachViewModel @Inject constructor(
    stateMachineFactory: AiCoachStateMachine,
) : ViewModel() {

    private val machine: FlowReduxStateMachine<StateFlow<AiCoachUiState>, AiCoachUiAction> by lazy {
        stateMachineFactory.launchIn(viewModelScope)
    }

    val state: StateFlow<AiCoachUiState>
        get() = machine.state

    // ---------------------------------------------------------------- A2UI data layer

    /** JSON parser for incoming A2UI protocol messages (agent -> client). */
    private val a2uiParser = A2uiMessageParser()

    /**
     * The A2UI message processor hosting the DailyFitness catalog.
     *
     * Owned by the ViewModel, which is activity-scoped, so surfaces survive bottom-tab
     * switches together with the in-memory conversation. Agent messages are pushed in by
     * [onAgentMessages]; the rendered surfaces come out of [a2uiSurfaces].
     */
    private val a2uiProcessor = A2uiMessageProcessor(
        catalogs = listOf(DailyFitnessA2uiCatalog.catalog),
    )

    /** Active A2UI surfaces emitted by the agent-driven UI branch. */
    val a2uiSurfaces: StateFlow<List<A2uiSurfaceModel>> = a2uiProcessor.activeSurfaces

    private val adoptRequestsChannel = Channel<CoachSuggestion>(Channel.BUFFERED)

    /**
     * One-shot adopt requests produced by tapping a button on an agent-authored surface.
     *
     * Exposed as a channel-backed flow because adopting is an event, not state: the host page
     * navigates and the signal must not be replayed on reconfiguration.
     */
    val adoptRequests: Flow<CoachSuggestion> = adoptRequestsChannel.receiveAsFlow()

    init {
        // Collecting keeps the machine running (stateIn is Lazily) even before Compose
        // subscribes; the state itself carries the conversation. The same loop feeds every
        // freshly committed agent-authored turn into the A2UI processor, deduplicated by
        // turn id so a re-emission of the state never re-applies a surface.
        viewModelScope.launch {
            val fedTurns = mutableSetOf<Int>()
            machine.state.collect { state ->
                state.history.forEach { message ->
                    val content = message.content
                    val turnId = message.turnId
                    if (content is CoachMessageContent.AgentUi && turnId != null && fedTurns.add(turnId)) {
                        onAgentMessages(content.messages)
                    }
                }
            }
        }
        // Drives the A2UI processor's internal channel; must stay alive with the ViewModel.
        viewModelScope.launch(Dispatchers.Default) {
            a2uiProcessor.collectMessages()
        }
        // Turns agent-authored surface actions back into the app's own navigation intent.
        viewModelScope.launch {
            a2uiProcessor.outboundEvents.collect { message ->
                DailyFitnessA2uiActions.suggestionOf(message)?.let { adoptRequestsChannel.send(it) }
            }
        }
    }

    /**
     * Feeds raw A2UI protocol messages (one JSON object per entry) produced by the
     * agent into the surface processor. Called by the state machine's A2UI branch.
     */
    fun onAgentMessages(messages: List<String>) {
        messages.forEach { json -> a2uiProcessor.processInput(a2uiParser, json) }
    }

    fun refresh() {
        machine.dispatchAction(AiCoachUiAction.Refresh)
    }

    /** Aborts the in-flight request; the machine surfaces it as a cancelled error state. */
    fun cancel() {
        machine.dispatchAction(AiCoachUiAction.Cancel)
    }
}
