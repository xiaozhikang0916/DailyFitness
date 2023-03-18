package site.xiaozk.dailyfitness.page.body.add

import android.util.ArrayMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
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
    val bodyField: BodyField = BodyField(),
) {

    val valid: Boolean
        get() = bodyField.valid

    fun toDailyBodyData(): BodyDataRecord {
        if (valid) {
            return BodyDataRecord(
                id = 0,
                instant = Instant.now(),
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
value class BodyField(
    val valueMap: ArrayMap<InputField, String> = ArrayMap(InputField.values().size),
) {
    fun getField(field: InputField): String {
        return valueMap[field] ?: ""
    }

    fun checkFieldValid(field: InputField): Boolean {
        val str = valueMap[field]
        return if (str.isNullOrBlank()) {
            true
        } else {
            val num = str.toFloatOrNull()
            if (num != null) {
                if (field == InputField.BodyFat) {
                    num in 0f..100f
                } else {
                    num >= 0f
                }
            } else {
                false
            }
        }
    }

    val weight: String
        get() = valueMap[InputField.Weight] ?: ""
    val bustSize: String
        get() = valueMap[InputField.Bust] ?: ""
    val waistSize: String
        get() = valueMap[InputField.Waist] ?: ""
    val hipSize: String
        get() = valueMap[InputField.Hip] ?: ""
    val bodyFat: String
        get() = valueMap[InputField.BodyFat] ?: ""

    fun copy(mapBuilder: (MutableMap<InputField, String>) -> Unit): BodyField {
        return BodyField(
            ArrayMap(this.valueMap).also(mapBuilder)
        )
    }

    val valid: Boolean
        get() = InputField.values().all(::checkFieldValid) &&
            valueMap.isNotEmpty() &&
            valueMap.values.any {
                it.isNullOrBlank().not()
            }
}

enum class InputField(val label: String, val trailing: String) {
    Weight("体重", "kg"),
    Bust("胸围", "cm"),
    Waist("腰围", "cm"),
    Hip("臀围", "cm"),
    BodyFat("体脂率", "%");
}