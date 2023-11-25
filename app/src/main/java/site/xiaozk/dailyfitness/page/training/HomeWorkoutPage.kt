package site.xiaozk.dailyfitness.page.training

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.datetime.toJavaLocalDate
import site.xiaozk.calendar.Calendar
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.HomepageScaffoldState
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.WorkoutStaticGroup
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
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
fun HomeWorkoutPage() {
    val homeViewModel: HomeWorkoutPageViewModel = hiltViewModel()
    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    val title = stringResource(R.string.title_home_workout)
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            HomepageScaffoldState(
                title = title,
            )
        )
    }
    val workoutDayList = homeViewModel.pageData.collectAsState()
    HomeWorkoutPage(state = workoutDayList.value, onNav = { appScaffoldViewModel.onRoute(it) })
}

@Composable
fun HomeWorkoutPage(state: HomeWorkoutPageState, onNav: (String) -> Unit) {
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    val scaffoldProperty = LocalScaffoldProperty.current
    val data = state.homePageState
    val monthData = data.monthStatic
    val routeToMonthSummary = Modifier.clickable {
        onNav(WorkoutStaticGroup.WorkoutMonthNavItem.getRoute(monthData.month))
    }
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
                displayMonth = monthData.displayMonth,
                modifier = routeToMonthSummary,
                showOverlappingDays = false,
                showMonthNavigator = false,
            ) {
                DayWithWorkout(day = it, workout = monthData[it.date])
            }
        }
        if (state.loadStatus != ActionStatus.Idle) {
            item(key = "month") {
                HomeWorkoutStaticsCard(
                    title = stringResource(R.string.label_home_workout_count_monthly),
                    content = monthData.monthTrainedDay.toString(),
                    subContent = stringResource(id = R.string.label_home_workout_count_unit),
                    modifier = routeToMonthSummary
                )
            }
            item(key = "week") {
                HomeWorkoutStaticsCard(
                    title = stringResource(R.string.label_home_workout_count_weekly),
                    content = monthData.getWeekTrainedDay().toString(),
                    subContent = stringResource(id = R.string.label_home_workout_count_unit),
                )
            }
        }
        data.weight?.let {
            item(key = "weight") {
                HomeWorkoutStaticsCard(
                    title = stringResource(R.string.label_home_workout_latest_weight),
                    content = it.data.second.toString(),
                    subContent = stringResource(site.xiaozk.dailyfitness.repository.R.string.label_weight_unit_kg),
                    bottom = it.data.first.toJavaLocalDate().format(formatter)
                )
            }
        }
        data.bustSize?.let {
            item(key = "bust") {
                HomeWorkoutStaticsCard(
                    title = stringResource(R.string.label_home_workout_latest_bust_size),
                    content = it.data.second.toString(),
                    subContent = stringResource(site.xiaozk.dailyfitness.repository.R.string.label_length_unit_cm),
                    bottom = it.data.first.toJavaLocalDate().format(formatter)
                )
            }
        }
        data.waistSize?.let {
            item(key = "waist") {
                HomeWorkoutStaticsCard(
                    title = stringResource(R.string.label_home_workout_latest_waist_size),
                    content = it.data.second.toString(),
                    subContent = stringResource(site.xiaozk.dailyfitness.repository.R.string.label_length_unit_cm),
                    bottom = it.data.first.toJavaLocalDate().format(formatter)
                )
            }
        }
        data.hipSize?.let {
            item(key = "hip") {
                HomeWorkoutStaticsCard(
                    title = stringResource(R.string.label_home_workout_latest_hip_size),
                    content = it.data.second.toString(),
                    subContent = stringResource(site.xiaozk.dailyfitness.repository.R.string.label_length_unit_cm),
                    bottom = it.data.first.toJavaLocalDate().format(formatter)
                )
            }
        }
        data.bodyFat?.let {
            item(key = "fat") {
                HomeWorkoutStaticsCard(
                    title = stringResource(R.string.label_home_workout_latest_body_fat),
                    content = it.data.second.toString(),
                    subContent = stringResource(site.xiaozk.dailyfitness.repository.R.string.label_count_unit_percentage),
                    bottom = it.data.first.toJavaLocalDate().format(formatter)
                )
            }
        }
    }
}