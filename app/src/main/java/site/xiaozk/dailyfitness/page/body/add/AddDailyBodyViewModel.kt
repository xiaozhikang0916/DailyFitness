package site.xiaozk.dailyfitness.page.body.add

import android.util.ArrayMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.BodyField
import java.time.Instant
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */
@HiltViewModel
class AddDailyBodyViewModel @Inject constructor(
    private val reducer: AddDailyBodyReducer,
) : ViewModel() {
    private val _stateFlow = MutableStateFlow(AddDailyBodyState())

    val stateFlow = _stateFlow.asStateFlow()
    fun reduce(intent: IDailyBodyIntent) {
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

data class AddDailyBodyState(
    val submitStatus: ActionStatus = ActionStatus.Idle,
    val bodyField: BodyFieldInput = BodyFieldInput(),
) {

    val valid: Boolean
        get() = bodyField.valid

    fun toDailyBodyData(): BodyDataRecord {
        if (valid) {
            return BodyDataRecord(
                id = 0,
                instant = Clock.System.now(),
                weight = bodyField.weight.toFloatOrNull() ?: 0f,
                bustSize = bodyField.bustSize.toFloatOrNull() ?: 0f,
                waistSize = bodyField.waistSize.toFloatOrNull() ?: 0f,
                hipSize = bodyField.hipSize.toFloatOrNull() ?: 0f,
                bodyFat = bodyField.bodyFat.toFloatOrNull() ?: 0f,
            )
        } else {
            throw IllegalArgumentException("Some of your input field is invalid")
        }
    }
}

@JvmInline
value class BodyFieldInput(
    val valueMap: ArrayMap<BodyField, String> = ArrayMap(BodyField.values().size),
) {
    fun getField(field: BodyField): String {
        return valueMap[field] ?: ""
    }

    fun checkFieldValid(field: BodyField): Boolean {
        val str = valueMap[field]
        return if (str.isNullOrBlank()) {
            true
        } else {
            val num = str.toFloatOrNull()
            if (num != null) {
                num in field.fieldRange
            } else {
                false
            }
        }
    }

    val weight: String
        get() = valueMap[BodyField.Weight] ?: ""
    val bustSize: String
        get() = valueMap[BodyField.Bust] ?: ""
    val waistSize: String
        get() = valueMap[BodyField.Waist] ?: ""
    val hipSize: String
        get() = valueMap[BodyField.Hip] ?: ""
    val bodyFat: String
        get() = valueMap[BodyField.BodyFat] ?: ""

    fun copy(mapBuilder: (MutableMap<BodyField, String>) -> Unit): BodyFieldInput {
        return BodyFieldInput(
            ArrayMap(this.valueMap).also(mapBuilder)
        )
    }

    val valid: Boolean
        get() = BodyField.values().all(::checkFieldValid) &&
            valueMap.isNotEmpty() &&
            valueMap.values.any {
                it.isNullOrBlank().not()
            }
}
