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
 * Activity-scoped tab ViewModel launching the FlowRedux [AiCoachStateMachine]
 * factory in the ViewModel scope.
 *
 * The ViewModel itself holds no conversation state: the machine's [AiCoachUiState]
 * carries everything (including the in-memory multi-turn history), which is why
 * an activity-scoped VM is enough for the chat to survive bottom-tab switches.
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
        // Collecting keeps the machine running (stateIn is Lazily) even before Compose
        // subscribes; the state itself carries the conversation.
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
