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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.datetime.toJavaLocalDate
import site.xiaozk.calendar.Calendar
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.LoadFailedSnackbar
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.Route
import site.xiaozk.dailyfitness.nav.SubpageScaffoldState
import site.xiaozk.dailyfitness.nav.TopAction
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.nav.WorkoutStaticGroup
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutSummary
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.YearMonth
import site.xiaozk.dailyfitness.utils.getLocalDateFormatter
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
fun WorkoutMonthlyPage() {
    val viewModel: WorkoutMonthlyPageViewModel = hiltViewModel()
    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    val title = stringResource(R.string.title_workout_monthly)
    val descAdd = stringResource(R.string.desc_top_action_add_action)
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            SubpageScaffoldState(
                title = title,
                actionItems = listOf(
                    TopAction.iconRouteAction(
                        Icons.Default.Add,
                        descAdd,
                        Route(TrainingDayGroup.TrainDayAddActionNavItem.route)
                    )
                )
            )
        )
    }
    val page = viewModel.workoutMonthPageState.collectAsState()
    LaunchedEffect(key1 = page.value.loadStatus) {
        if (page.value.loadStatus is ActionStatus.Failed) {
            appScaffoldViewModel.showSnackbar(LoadFailedSnackbar)
        }
    }
    WorkoutMonthlyPage(
        page = page.value.monthData,
        onMonthChanged = {
            viewModel.month = (it)
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
    val formatter = remember {
        DateTimeFormatterBuilder()
            .append(getLocalDateFormatter())
            .appendLiteral(" ")
            .appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL)
            .toFormatter(Locale.getDefault())
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
        items(list, key = { it.date.toString() }) {
            WorkoutDailyDetailCard(
                data = it,
                onNav = onNav,
                dateTimeFormatter = formatter,
            )
        }
    }
}

@Composable
private fun WorkoutDailyDetailCard(
    data: DailyWorkoutSummary,
    dateTimeFormatter: DateTimeFormatter ,
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
            text = dateTimeFormatter.format(data.date.toJavaLocalDate()),
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
                    text = stringResource(R.string.label_train_part_unit, it.value.size),
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
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _month: StateFlow<YearMonth> = savedStateHandle.getStateFlow("date", "")
        .map {
            WorkoutStaticGroup.WorkoutMonthNavItem.fromArgument(it)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, YearMonth.now())

    var month: YearMonth
        get() = _month.value
        set(value) {
            savedStateHandle["date"] = WorkoutStaticGroup.WorkoutMonthNavItem.parseArgument(value)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val workoutMonthPageState: StateFlow<MonthWorkoutPageState> = _month.transformLatest {
        Log.i("WorkoutMonthlyPage", "new month collected $it")
        val user = userRepository.getCurrentUser()
        emit(MonthWorkoutPageState(month = it, loadStatus = ActionStatus.Loading))
        emitAll(
            homeRepo.getMonthWorkoutStatic(
                user = user,
                month = it,
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