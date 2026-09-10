package site.xiaozk.dailyfitness.page.training.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freeletics.flowredux2.FlowReduxStateMachine
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.aicoach.engine.CoachSuggestion

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
@HiltViewModel(assistedFactory = AddDailyWorkoutViewModel.Factory::class)
class AddDailyWorkoutViewModel @AssistedInject constructor(
    stateMachineFactory: AddWorkoutStateMachine.Factory,
    @Assisted suggestion: CoachSuggestion?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(suggestion: CoachSuggestion?): AddDailyWorkoutViewModel
    }

    private val machine: FlowReduxStateMachine<StateFlow<AddWorkoutUiState>, AddWorkoutAction> =
        stateMachineFactory.create(suggestion).launchIn(viewModelScope)

    val state: StateFlow<AddWorkoutUiState>
        get() = machine.state

    init {
        // Keep the machine running even before Compose subscribes.
        viewModelScope.launch { machine.state.collect { } }
    }

    fun dispatch(action: AddWorkoutAction) {
        machine.dispatchAction(action)
    }
}
