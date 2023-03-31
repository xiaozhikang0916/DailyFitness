package site.xiaozk.dailyfitness.page.training

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.HomepageScaffoldState
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.WorkoutDayList
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
    val workoutDayList = homeViewModel.pageData.collectAsState(initial = WorkoutDayList())
    TrainingHome(data = workoutDayList.value, onNav = { appScaffoldViewModel.onRoute(it) })
}

@Composable
fun TrainingHome(data: WorkoutDayList, onNav: (String) -> Unit) {
    val dates = data.trainedDate.values.sortedBy { it.date }
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    val scaffoldProperty = LocalScaffoldProperty.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scaffoldProperty.scrollConnection),
        contentPadding = scaffoldProperty.padding,
    ) {
        items(dates) {
            HomePageTrainedDay(day = it, format = formatter) { trainDay ->
                onNav(TrainingDayGroup.TrainDayNavItem.getRoute(trainDay.date))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomePageTrainedDay(
    day: DailyWorkout,
    format: DateTimeFormatter,
    onCardClick: (DailyWorkout) -> Unit,
) {
    Card(
        onClick = { onCardClick(day) },
        modifier = Modifier
            .padding(all = 6.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = format.format(day.date),
            fontSize = 14.sp,
        )

//        day.maxTrainedParts?.let {
//            Text(
//                text = it,
//                modifier = Modifier.padding(top = 4.dp),
//                fontSize = 12.sp,
//            )
//        }
    }
}