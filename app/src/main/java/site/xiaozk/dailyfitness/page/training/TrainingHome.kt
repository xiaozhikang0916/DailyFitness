@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.training

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.AppHomeRootNav
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.nav.updateAppScaffoldState
import site.xiaozk.dailyfitness.repository.model.TrainingDayList
import site.xiaozk.dailyfitness.repository.model.TrainingDayData
import java.time.LocalDate
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
    val localNav = LocalNavController.current
    localNav?.navController?.let {
        AppHomeRootNav.AppHomePage.TrainingHomeNavItem.updateAppScaffoldState(
            navController = it
        ) {
            fabRoute = TrainingDayGroup.TrainDayNavItem.getRoute(LocalDate.now())
            showBottomNavBar = true
        }
    }
    val trainingDayList = homeViewModel.pageData.collectAsState(initial = TrainingDayList())
    TrainingHome(data = trainingDayList.value, onNav = { localNav?.navigate(it) })
}

@Composable
fun TrainingHome(data: TrainingDayList, onNav: (String) -> Unit) {
    val dates = data.trainedDate.values.sortedBy { it.date }
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(dates) {
                HomePageTrainedDay(day = it, format = formatter) { trainDay ->
                    onNav(TrainingDayGroup.TrainDayNavItem.getRoute(trainDay.date))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomePageTrainedDay(
    day: TrainingDayData,
    format: DateTimeFormatter,
    onCardClick: (TrainingDayData) -> Unit,
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

        day.maxTrainedParts?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 4.dp),
                fontSize = 12.sp,
            )
        }
    }
}