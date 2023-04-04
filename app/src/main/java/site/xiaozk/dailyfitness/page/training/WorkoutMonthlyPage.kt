package site.xiaozk.dailyfitness.page.training

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import site.xiaozk.calendar.Calendar
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.SnackbarStatus
import site.xiaozk.dailyfitness.nav.SubpageScaffoldState
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutSummary
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @create: 2023/4/3
 */

@Composable
fun WorkoutMonthlyPage(initMonth: YearMonth = YearMonth.now()) {
    val viewModel: WorkoutMonthlyPageViewModel = hiltViewModel()
    val appScaffoldViewModel: AppScaffoldViewModel = hiltViewModel()
    LaunchedEffect(key1 = initMonth) {
        viewModel.month.emit(initMonth)
    }
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            SubpageScaffoldState(
                title = "训练月报",
            )
        )
    }
    val page = viewModel.workoutMonthPageState.collectAsState()
    LaunchedEffect(key1 = page.value.loadStatus) {
        if (page.value.loadStatus is ActionStatus.Failed) {
            appScaffoldViewModel.showSnackbar("加载失败", status = SnackbarStatus.Error)
        }
    }
    WorkoutMonthlyPage(
        page = page.value.monthData,
        onMonthChanged = {
            val result = viewModel.month.tryEmit(it)
            Log.i("WorkoutMonthlyPage", "change month to $it, result $result")
        },
        onNav = { appScaffoldViewModel.onRoute(it) }
    )
}

@Composable
fun WorkoutMonthlyPage(page: MonthWorkoutStatic, onMonthChanged: (YearMonth) -> Unit, onNav: (String) -> Unit) {
    val scaffoldProperty = LocalScaffoldProperty.current
    val list = remember(page) {
        page.workoutDays.trainedDate.descendingMap().values.toList()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 12.dp)
            .nestedScroll(scaffoldProperty.scrollConnection),
        contentPadding = scaffoldProperty.padding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "calendar") {
            Calendar(
                displayMonth = page.displayMonth,
                showOverlappingDays = false,
                onMonthChanged = onMonthChanged,
            ) {
                DayWithWorkout(day = it, workout = page.workoutDays[it.date])
            }
        }
        items(list, key = { it.date }) {
            WorkoutDailyDetailCard(
                data = it,
                onNav = onNav,
            )
        }
    }
}

@Composable
private fun WorkoutDailyDetailCard(
    data: DailyWorkoutSummary,
    dateTimeFormatter: DateTimeFormatter = remember {
        DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_DATE)
            .appendLiteral(" ")
            .appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL)
            .toFormatter(Locale.getDefault())
    },
    onNav: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNav(TrainingDayGroup.TrainDayNavItem.getRoute(data.date))
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = dateTimeFormatter.format(data.date),
            style = MaterialTheme.typography.titleMedium
        )
        data.partsGroup.forEach {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = it.key.partName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${it.value.size}组",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@HiltViewModel
class WorkoutMonthlyPageViewModel @Inject constructor(
    private val homeRepo: IDailyWorkoutRepository,
    private val userRepository: IUserRepository,
) : ViewModel() {
    val month: MutableStateFlow<YearMonth> = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val workoutMonthPageState: StateFlow<MonthWorkoutPageState> = month.transformLatest {
        Log.i("WorkoutMonthlyPage", "new month collected $it")
        val user = userRepository.getCurrentUser()
        emit(MonthWorkoutPageState(month = it, loadStatus = ActionStatus.Loading))
        emitAll(
            homeRepo.getMonthWorkoutStatic(
                user = user,
                month = it,
                firstDayOfWeek = DayOfWeek.SUNDAY,
            ).catch { e ->
                Log.e("WorkoutMonthlyPage", "load month summary of $it failed", e)
                emit(MonthWorkoutPageState(month = it, loadStatus = ActionStatus.Failed(e)))
            }.map { data ->
                MonthWorkoutPageState(
                    monthData = data,
                    loadStatus = ActionStatus.Done
                )
            }
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = MonthWorkoutPageState())
}

data class MonthWorkoutPageState(
    val monthData: MonthWorkoutStatic = MonthWorkoutStatic(YearMonth.now()),
    val loadStatus: ActionStatus = ActionStatus.Idle,
) {
    constructor(month: YearMonth, loadStatus: ActionStatus) : this(MonthWorkoutStatic(month), loadStatus)
}