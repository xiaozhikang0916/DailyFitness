package site.xiaozk.dailyfitness.aicoach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freeletics.flowredux2.FlowReduxStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.AiCoachModel
import javax.inject.Inject

/**
 * Tab ViewModel launching the FlowRedux [AiCoachStateMachine] factory in the
 * ViewModel scope.
 *
 * All state transitions live in the machine; this class only exposes its StateFlow
 * to Compose and feeds the "sets recorded today" live update (from the DB flow)
 * into the machine as [AiCoachUiAction.TodayInfo].
 */
@HiltViewModel
class AiCoachViewModel @Inject constructor(
    private val stateMachineFactory: AiCoachStateMachine,
    private val userRepository: IUserRepository,
    private val workoutRepository: IDailyWorkoutRepository,
) : ViewModel() {

    private val machine: FlowReduxStateMachine<StateFlow<AiCoachUiState>, AiCoachUiAction> by lazy {
        stateMachineFactory.launchIn(viewModelScope)
    }

    val state: StateFlow<AiCoachUiState>
        get() = machine.state

    init {
        // Keep the machine running (stateIn is Lazily) regardless of UI subscription
        // so early TodayInfo dispatches are not dropped.
        viewModelScope.launch {
            machine.state.collect { }
        }
        viewModelScope.launch { observeTodaySets() }
    }

    fun refresh() {
        machine.dispatchAction(AiCoachUiAction.Refresh)
    }

    fun saveConfig(apiKey: String, model: AiCoachModel) {
        machine.dispatchAction(AiCoachUiAction.SaveConfig(apiKey = apiKey, model = model))
    }

    private suspend fun observeTodaySets() {
        val user = userRepository.getCurrentUser()
        val today = todayLocalDate()
        workoutRepository.getWorkoutOfDayFlow(user, today)
            .map { workout -> workout?.actions?.sumOf { it.trainAction.size } ?: 0 }
            .distinctUntilChanged()
            .collect { sets -> machine.dispatchAction(AiCoachUiAction.TodayInfo(sets)) }
    }
}

private fun todayLocalDate(): LocalDate =
    kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
