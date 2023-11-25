package site.xiaozk.dailyfitness.page.body

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaInstant
import site.xiaozk.chart.BodyChartLine
import site.xiaozk.chart.BodyChartPoint
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.repository.IPersonDailyRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import site.xiaozk.dailyfitness.repository.model.BodyField
import site.xiaozk.dailyfitness.repository.model.YearMonth
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BodyViewModel @Inject constructor(
    private val bodyRepo: IPersonDailyRepository,
    private val userRepo: IUserRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _month: StateFlow<YearMonth> = savedStateHandle.getStateFlow("date", YearMonth.now())

    var month: YearMonth
        get() = _month.value
        set(value) {
            savedStateHandle["date"] = value
        }

    private val _field: StateFlow<BodyField> = savedStateHandle.getStateFlow("field", BodyField.Weight)

    var field: BodyField
        get() = _field.value
        set(value) {
            savedStateHandle["field"] = value
        }

    private val bodyDetailFlow: StateFlow<BodyDetailLoadState> = _month.transformLatest { month ->
        val user = userRepo.getCurrentUser()
        emitAll(
            bodyRepo.getPersonDailyDataFlow(
                user,
                month.atDay(1),
                month.atEndOfMonth()
            ).map { data ->
                BodyDetailLoadState(list = data, loadStatus = ActionStatus.Done, month = month)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, BodyDetailLoadState())

    val bodyDetail: StateFlow<BodyDetailPageState> = combine(
        bodyDetailFlow,
        _field,
    ) { body, field ->
        BodyDetailPageState(loadState = body, selectedField = field)
    }.stateIn(viewModelScope, SharingStarted.Lazily, BodyDetailPageState())


    private val _deleteAction = MutableStateFlow<ActionStatus>(ActionStatus.Idle)
    val deleteAction = _deleteAction.asStateFlow()

    fun deleteBodyDetail(data: BodyDataRecord) {
        viewModelScope.launch {
            _deleteAction.emit(ActionStatus.Loading)
            try {
                bodyRepo.removePersonDailyData(data)
                _deleteAction.emit(ActionStatus.Done)
            } catch (e: Exception) {
                _deleteAction.emit(ActionStatus.Failed(e))
            }
        }
    }

}

data class BodyDetailLoadState(
    val list: BodyDataWithDate = BodyDataWithDate(),
    val loadStatus: ActionStatus = ActionStatus.Idle,
    val month: YearMonth = YearMonth.now(),
)

data class BodyDetailPageState(
    val loadState: BodyDetailLoadState = BodyDetailLoadState(),
    val selectedField: BodyField = BodyField.Weight,
) {
    val list: BodyDataWithDate
        get() = loadState.list
    val loadStatus: ActionStatus
        get() = loadState.loadStatus
    val month: YearMonth
        get() = loadState.month

    val chartLine: BodyChartLine = list.personData.entries.flatMap { it.value }
        .map {
            BodyChartPoint(
                it.instant.toJavaInstant(),
                selectedField.property(it)
            )
        }.let(::BodyChartLine)

    fun hasFieldData(field: BodyField): Boolean {
        return list.personData.values.any { it.any { v -> field.property(v) > 0 } }
    }
}
