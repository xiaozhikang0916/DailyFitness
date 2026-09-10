package site.xiaozk.dailyfitness.aicoach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freeletics.flowredux2.FlowReduxStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.AiCoachModel
import javax.inject.Inject

/**
 * Activity-scoped tab ViewModel launching the FlowRedux [AiCoachStateMachine]
 * factory in the ViewModel scope.
 *
 * The conversation lives in memory only ([history]): it survives tab switches
 * (activity-scoped VM) but is intentionally not persisted. The engine stays
 * stateless - every request gets the current history via
 * [AiCoachUiAction.Refresh] and returns the new turn(s) which are appended here.
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

    private val _history = MutableStateFlow<List<CoachMessage>>(emptyList())

    /** In-memory conversation (newest last), capped at [HISTORY_MESSAGES]. */
    val history: StateFlow<List<CoachMessage>> = _history.asStateFlow()

    val state: StateFlow<AiCoachUiState>
        get() = machine.state

    init {
        // Collecting both keeps the machine running (stateIn is Lazily) and appends
        // the turn produced by each successful request to the session memory.
        viewModelScope.launch {
            machine.state.collect { state ->
                if (state is AiCoachUiState.Idle) appendNewMessages(state.content)
            }
        }
        viewModelScope.launch { observeTodaySets() }
    }

    fun refresh() {
        machine.dispatchAction(AiCoachUiAction.Refresh(history = _history.value))
    }

    fun saveConfig(apiKey: String, model: AiCoachModel) {
        machine.dispatchAction(AiCoachUiAction.SaveConfig(apiKey = apiKey, model = model))
    }

    private fun appendNewMessages(content: UiContent?) {
        val newMessages = when (content) {
            is UiContent.TodayPlan -> content.newMessages
            is UiContent.NextAdvice -> content.newMessages
            else -> emptyList()
        }
        if (newMessages.isEmpty()) return
        val current = _history.value
        if (current.size >= newMessages.size && current.takeLast(newMessages.size) == newMessages) return
        _history.value = (current + newMessages).takeLast(HISTORY_MESSAGES)
    }

    private suspend fun observeTodaySets() {
        val user = userRepository.getCurrentUser()
        val today = todayLocalDate()
        workoutRepository.getWorkoutOfDayFlow(user, today)
            .map { workout -> workout?.actions?.sumOf { it.trainAction.size } ?: 0 }
            .distinctUntilChanged()
            .collect { sets -> machine.dispatchAction(AiCoachUiAction.TodayInfo(sets)) }
    }

    private companion object {
        /** 5 rounds = 10 messages kept in session memory (and sent back to the LLM). */
        const val HISTORY_MESSAGES = 10
    }
}

private fun todayLocalDate(): LocalDate =
    kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
