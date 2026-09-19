package site.xiaozk.dailyfitness.aicoach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freeletics.flowredux2.FlowReduxStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    init {
        // Collecting keeps the machine running (stateIn is Lazily) even before Compose
        // subscribes; the state itself carries the conversation.
        viewModelScope.launch {
            machine.state.collect { }
        }
    }

    fun refresh() {
        machine.dispatchAction(AiCoachUiAction.Refresh)
    }

    /** Aborts the in-flight request; the machine surfaces it as a cancelled error state. */
    fun cancel() {
        machine.dispatchAction(AiCoachUiAction.Cancel)
    }
}
