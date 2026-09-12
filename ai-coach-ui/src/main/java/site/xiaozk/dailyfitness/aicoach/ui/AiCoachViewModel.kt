package site.xiaozk.dailyfitness.aicoach.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freeletics.flowredux2.FlowReduxStateMachine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessageContent
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import javax.inject.Inject

/**
 * Activity-scoped tab ViewModel launching the FlowRedux [AiCoachStateMachine]
 * factory in the ViewModel scope.
 *
 * The ViewModel itself holds no conversation state: the machine's [AiCoachUiState]
 * carries everything (including the in-memory multi-turn history), which is why
 * an activity-scoped VM is enough for the chat to survive bottom-tab switches.
 *
 * The user turn of each request is built here (it only needs today's training
 * state, all client-side data) so it can be rendered before the LLM round-trip.
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

    /** Latest observed today-training state, used to build the user turn locally. */
    private var todaySets: Int = 0
    private var todayCurrentPart: String? = null

    init {
        // Collecting keeps the machine running (stateIn is Lazily) even before Compose
        // subscribes; the state itself carries the conversation.
        viewModelScope.launch {
            machine.state.collect { }
        }
        viewModelScope.launch { observeToday() }
    }

    fun refresh() {
        machine.dispatchAction(AiCoachUiAction.Refresh(buildUserContent()))
    }

    private fun buildUserContent(): CoachMessageContent {
        val today = todayLocalDate()
        return if (todaySets <= 0) {
            CoachMessageContent.PlanRequest(today)
        } else {
            CoachMessageContent.AdviceRequest(
                date = today,
                setsToday = todaySets,
                partName = todayCurrentPart.orEmpty(),
            )
        }
    }

    private suspend fun observeToday() {
        val user = userRepository.getCurrentUser()
        val today = todayLocalDate()
        workoutRepository.getWorkoutOfDayFlow(user, today)
            .distinctUntilChanged()
            .collect { workout ->
                todaySets = workout?.actions?.sumOf { it.trainAction.size } ?: 0
                todayCurrentPart = workout?.actions
                    ?.filter { action -> action.trainAction.isNotEmpty() }
                    ?.maxByOrNull { action -> action.trainAction.maxOf { it.instant } }
                    ?.action?.part?.partName
                machine.dispatchAction(AiCoachUiAction.TodayInfo(todaySets))
            }
    }
}

private fun todayLocalDate(): LocalDate =
    kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
