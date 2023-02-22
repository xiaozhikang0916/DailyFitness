package site.xiaozk.dailyfitness.page.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.repository.model.DailyTrainingPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainingDayData
import site.xiaozk.dailyfitness.widget.BackButton
import site.xiaozk.dailyfitness.widget.FloatingActionButtonShowHide
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/25
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingDayDetailPage(
    date: LocalDate,
) {
    val viewModel: TrainingDayDetailViewModel = hiltViewModel()
    val data = viewModel.getTrainingData(date)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val nav = LocalNavController.current
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "训练日志") },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButtonShowHide(
                onClick = { nav?.navigate(TrainingDayGroup.TrainDayAddActionNavItem.route) },
                topAppBarState = scrollBehavior.state
            ) {
                Icon(painter = rememberVectorPainter(image = Icons.Default.Create), contentDescription = "add")
            }
        }
    ) {
        TrainingDayDetail(
            data = data.collectAsState(initial = TrainingDayData(date)).value,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            padding = it,
        )
    }
}

@Composable
fun TrainingDayDetail(
    data: TrainingDayData,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = padding,
    ) {
        item {
            Text(
                text = dateFormat.format(data.date),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 4.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
        items(data.trainedParts) {
            DailyTrainingPart(group = it)
        }
    }
}

@Composable
private fun DailyTrainingPart(group: DailyTrainingPartGroup) {
    Card(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = group.trainPart.partName,
            modifier = Modifier
                .padding(all = 4.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall
        )
        group.actions.forEach { group ->
            Text(text = group.action.actionName, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
            group.trainAction.map { it.displayText }.forEach { action ->
                action.forEach {
                    Text(
                        text = it, modifier = Modifier
                            .padding(end = 4.dp)
                            .align(Alignment.End)
                    )
                }
            }
        }
    }
}