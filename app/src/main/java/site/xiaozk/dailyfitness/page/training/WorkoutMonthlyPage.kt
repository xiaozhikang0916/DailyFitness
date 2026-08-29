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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import kotlinx.datetime.toJavaLocalDate
import site.xiaozk.calendar.Calendar
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.LoadFailedSnackbar
import site.xiaozk.dailyfitness.nav.LocalAppSnackbarHostState
import site.xiaozk.dailyfitness.nav.LocalNavBackStack
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import site.xiaozk.dailyfitness.nav.AddWorkoutAction
import site.xiaozk.dailyfitness.nav.TrainDay
import site.xiaozk.dailyfitness.nav.WorkoutMonth
import androidx.navigation3.runtime.NavKey
import site.xiaozk.dailyfitness.widget.ScaffoldProperty
import site.xiaozk.dailyfitness.widget.SubPageScaffold
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutSummary
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import kotlinx.datetime.YearMonth
import site.xiaozk.dailyfitness.repository.model.now
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
fun WorkoutMonthlyPage(month: YearMonth) {
    val viewModel = hiltViewModel<WorkoutMonthlyPageViewModel, WorkoutMonthlyPageViewModel.Factory>(
        creationCallback = { it.create(month) }
    )
    val navBackStack = LocalNavBackStack.current
    val systemBack = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val appSnackbarHostState = LocalAppSnackbarHostState.current
    val title = stringResource(R.string.title_workout_monthly)
    val descAdd = stringResource(R.string.desc_top_action_add_action)
    val page = viewModel.workoutMonthPageState.collectAsState()
    LaunchedEffect(key1 = page.value.loadStatus) {
        if (page.value.loadStatus is ActionStatus.Failed) {
            appSnackbarHostState.showSnackbar(LoadFailedSnackbar)
        }
    }
    SubPageScaffold(
        title = title,
        onBack = { systemBack?.onBackPressed() },
        actions = {
            IconButton(
                onClick = { navBackStack.add(AddWorkoutAction) }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = descAdd)
            }
        }
    ) { scaffoldProperty ->
        WorkoutMonthlyPage(
            page = page.value.monthData,
            onMonthChanged = { viewModel.month = it },
            onNav = { navBackStack.add(it) },
            scaffoldProperty = scaffoldProperty,
        )
    }
}

@Composable
fun WorkoutMonthlyPage(
    page: MonthWorkoutStatic,
    onMonthChanged: (YearMonth) -> Unit,
    onNav: (NavKey) -> Unit,
    scaffoldProperty: ScaffoldProperty = ScaffoldProperty(),
) {
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
    onNav: (NavKey) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNav(TrainDay(date = data.date))
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

@HiltViewModel(assistedFactory = WorkoutMonthlyPageViewModel.Factory::class)
class WorkoutMonthlyPageViewModel @AssistedInject constructor(
    private val homeRepo: IDailyWorkoutRepository,
    private val userRepository: IUserRepository,
    @Assisted month: YearMonth,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(month: YearMonth): WorkoutMonthlyPageViewModel
    }

    private val _month: MutableStateFlow<YearMonth> = MutableStateFlow(month)

    var month: YearMonth
        get() = _month.value
        set(value) {
            _month.value = value
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