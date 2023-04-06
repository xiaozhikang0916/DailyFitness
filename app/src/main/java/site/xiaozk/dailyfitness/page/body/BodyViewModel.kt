package site.xiaozk.dailyfitness.page.body

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import site.xiaozk.chart.point.IPoint
import site.xiaozk.chart.point.Line
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.page.body.add.InputField
import site.xiaozk.dailyfitness.repository.IPersonDailyRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import java.text.DecimalFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, YearMonth.now())

    var month: YearMonth
        get() = _month.value
        set(value) {
            savedStateHandle["date"] = value
        }

    val bodyDetail: Flow<BodyDetailPageState> = _month.transformLatest {
        val user = userRepo.getCurrentUser()
        emitAll(
            bodyRepo.getPersonDailyDataFlow(
                user,
                it.atDay(1),
                it.atEndOfMonth()
            ).map { data ->
                BodyDetailPageState(
                    list = data,
                    month = it,
                    loadStatus = ActionStatus.Done,
                )
            }
        )
    }

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

data class BodyDetailPageState(
    val list: BodyDataWithDate = BodyDataWithDate(),
    val month: YearMonth = YearMonth.now(),
    val loadStatus: ActionStatus = ActionStatus.Idle,
    val selectedField: InputField = InputField.Weight,
) {
    val chartLine: Line = list.personData.entries.flatMap { it.value }
        .map {
            BodyChartPoint(
                it.instant,
                when (selectedField) {
                    InputField.Weight ->
                        it.weight

                    InputField.Bust ->
                        it.bustSize

                    InputField.Waist ->
                        it.waistSize

                    InputField.Hip ->
                        it.hipSize

                    InputField.BodyFat ->
                        it.bodyFat
                }
            )
        }.let(::Line)
}

private val bodyChartDateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

data class BodyChartPoint(
    val date: Instant,
    val value: Float,
) : IPoint {
    override val xValue: Float
        get() = date.toEpochMilli().toFloat()
    override val yValue: Float
        get() = value
    override val xDisplay: String
        get() = bodyChartDateFormat.format(date)
    override val yDisplay: String
        get() = DecimalFormat("#.#").format(value)
}