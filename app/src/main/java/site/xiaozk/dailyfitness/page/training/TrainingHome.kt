package site.xiaozk.dailyfitness.page.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.calendar.Calendar
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.HomepageScaffoldState
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/24
 */

/**
 * Display the list of daily training page,
 */
@Composable
fun TrainingHome() {
    val homeViewModel: TrainingHomeViewModel = hiltViewModel()
    val appScaffoldViewModel: AppScaffoldViewModel = hiltViewModel()
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            HomepageScaffoldState(
                title = "训练记录",
            )
        )
    }
    val workoutDayList = homeViewModel.pageData.collectAsState()
    TrainingHome(state = workoutDayList.value, onNav = { appScaffoldViewModel.onRoute(it) })
}

@Composable
fun TrainingHome(state: HomeWorkoutPageState, onNav: (String) -> Unit) {
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    val scaffoldProperty = LocalScaffoldProperty.current
    val data = state.homePageState
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .nestedScroll(scaffoldProperty.scrollConnection),
        contentPadding = PaddingValues(
            top = scaffoldProperty.padding.calculateTopPadding(),
            bottom = scaffoldProperty.padding.calculateBottomPadding() + 12.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "calendar", span = { GridItemSpan(2) }) {
            Calendar(
                displayMonth = data.displayMonth,
                showOverlappingDays = false,
                showMonthNavigator = false,
            ) {
                DayWithWorkout(day = it, showOverlappingDays = false, workout = data[it.date])
            }
        }
        if (state.loadStatus != ActionStatus.Idle) {
            item(key = "month") {
                HomeWorkoutStaticsCard(
                    title = "本月训练",
                    content = data.monthTrainedDay.toString(),
                    subContent = "次"
                )
            }
            item(key = "week") {
                HomeWorkoutStaticsCard(
                    title = "本周训练",
                    content = data.getWeekTrainedDay().toString(),
                    subContent = "次"
                )
            }
        }
        data.weight?.let {
            item(key = "weight") {
                HomeWorkoutStaticsCard(
                    title = "最近体重",
                    content = it.data.second.toString(),
                    subContent = "kg",
                    bottom = it.data.first.format(formatter)
                )
            }
        }
        data.bustSize?.let {
            item(key = "bust") {
                HomeWorkoutStaticsCard(
                    title = "最近胸围",
                    content = it.data.second.toString(),
                    subContent = "cm",
                    bottom = it.data.first.format(formatter)
                )
            }
        }
        data.waistSize?.let {
            item(key = "waist") {
                HomeWorkoutStaticsCard(
                    title = "最近腰围",
                    content = it.data.second.toString(),
                    subContent = "cm",
                    bottom = it.data.first.format(formatter)
                )
            }
        }
        data.hipSize?.let {
            item(key = "hip") {
                HomeWorkoutStaticsCard(
                    title = "最近臀围",
                    content = it.data.second.toString(),
                    subContent = "cm",
                    bottom = it.data.first.format(formatter)
                )
            }
        }
        data.bodyFat?.let {
            item(key = "fat") {
                HomeWorkoutStaticsCard(
                    title = "最近体脂率",
                    content = it.data.second.toString(),
                    subContent = "%",
                    bottom = it.data.first.format(formatter)
                )
            }
        }
    }
}