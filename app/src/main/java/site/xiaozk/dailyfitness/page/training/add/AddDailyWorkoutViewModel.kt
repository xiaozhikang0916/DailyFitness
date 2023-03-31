package site.xiaozk.dailyfitness.page.training.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit
import java.time.Instant
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
@HiltViewModel
class AddDailyWorkoutViewModel @Inject constructor(
    private val reducer: DailyWorkoutReducer,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(AddDailyWorkoutPageState())

    val stateFlow = _stateFlow.asStateFlow()

    init {
        reduce(LoadPartIntent)
    }

    fun reduce(intent: IDailyTrainIntent) {
        viewModelScope.launch {
            val current = stateFlow.value
            val result = reducer.reduce(current, intent)
            _stateFlow.emit(result.state)
            result.sideEffect.collect {
                reduce(it)
            }
        }
    }
}

data class AddDailyWorkoutPageState(
    val instant: Instant = Instant.now(),
    val allParts: List<TrainPartGroup> = emptyList(),
    val selectedPart: TrainPartGroup? = null,
    val selectedAction: TrainActionWithPart? = null,
    val showPartMenuState: Boolean = false,
    val showActionMenuState: Boolean = false,
    val duration: String = "",
    val weight: String = "",
    val count: String = "",
    val timeUnit: TimeUnit = TimeUnit.Sec,
    val weightUnit: WeightUnit = WeightUnit.Kg,
    val note: String = "",
    val submitStatus: ActionStatus = ActionStatus.Idle,
) {
    fun toDailyTrain(): DailyWorkoutAction {
        if (selectedAction != null && valid) {
            return DailyWorkoutAction(
                instant = instant,
                action = selectedAction.action,
                takenCount = count.toIntOrNull() ?: 0,
                takenDuration = duration.toFloatOrNull()?.let { RecordedDuration(it, timeUnit) },
                takenWeight = weight.toFloatOrNull()?.let { RecordedWeight(it, weightUnit) },
                note = note,
            )
        } else {
            throw IllegalStateException("Train action is not set")
        }
    }

    fun cleanInput(): AddDailyWorkoutPageState {
        return this.copy(
            duration = "",
            weight = "",
            count = "",
            timeUnit = TimeUnit.Sec,
            weightUnit = WeightUnit.Kg,
            note = "",
        )
    }

    val timeValid: Boolean
        get() = (duration.isBlank() || duration.toFloatOrNull() != null)
    val weightValid: Boolean
        get() = (weight.isBlank() || weight.toFloatOrNull() != null)
    val countValid: Boolean
        get() = (count.isBlank() || count.toIntOrNull() != null)

    val valid: Boolean
        get() {
            return timeValid && weightValid && countValid && selectedAction != null
        }
}