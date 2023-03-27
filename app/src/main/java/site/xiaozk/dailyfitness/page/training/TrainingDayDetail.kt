package site.xiaozk.dailyfitness.page.training

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.Route
import site.xiaozk.dailyfitness.nav.SubpageScaffoldState
import site.xiaozk.dailyfitness.nav.TopAction
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListActionPair
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/25
 */
@Composable
fun TrainingDayDetailPage(
    date: LocalDate,
    padding: PaddingValues = PaddingValues(),
) {
    val viewModel: TrainingDayDetailViewModel = hiltViewModel()
    val data = viewModel.getTrainingData(date)

    val appScaffoldViewModel: AppScaffoldViewModel = hiltViewModel()
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            SubpageScaffoldState(
                title = "训练日志",
                actionItems = listOf(
                    TopAction.iconRouteAction(Icons.Default.Add, "添加训练动作", Route(TrainingDayGroup.TrainDayAddActionNavItem.route))
                )
            )
        )
    }
    var deleteActionDialog by remember {
        mutableStateOf<DailyWorkoutAction?>(null)
    }
    TrainingDayDetail(
        data = data.collectAsState(initial = null).value ?: DailyWorkout(date),
        padding = padding,
        onTrainingActionDeleted = {
            deleteActionDialog = it
        }
    )

    deleteActionDialog?.let {
        val dismiss = {
            deleteActionDialog = null
        }
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                Text(
                    text = "删除",
                    modifier = Modifier
                        .clickable {
                            viewModel.removeTrainAction(it)
                            dismiss()
                        },
                    textAlign = TextAlign.Center
                )
            },
            dismissButton = {
                Text(
                    text = "取消",
                    modifier = Modifier
                        .clickable { dismiss() },
                    textAlign = TextAlign.Center
                )
            },
            title = {
                Text(text = "删除动作记录")
            },
            text = {
                val dateTimeFormat = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())
                Text(
                    text = "你将要删除记录于${dateTimeFormat.format(it.instant)}的动作记录${it.action.actionName} ${
                        it.displayText.joinToString(" ")
                    }"
                )
            }
        )
    }
}

@Composable
fun TrainingDayDetail(
    data: DailyWorkout,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    onTrainingActionDeleted: (DailyWorkoutAction) -> Unit = {},
) {
    val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = padding,
    ) {
        item {
            Text(
                text = dateFormat.format(data.date),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }
        itemsIndexed(data.actions) { index, it ->
            DailyTrainingAction(action = it, onActionDelete = onTrainingActionDeleted)
            if (index != data.actions.size - 1) {
                Divider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DailyTrainingAction(action: DailyWorkoutListActionPair, onActionDelete: (DailyWorkoutAction) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = action.action.actionName,
                modifier = Modifier.align(Alignment.TopStart),
                style = MaterialTheme.typography.bodyLarge,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(top = 18.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.End,
            ) {
                action.trainAction.forEach { action ->
                    Text(
                        text = action.displayText.joinToString(separator = " "),
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    onActionDelete(action)
                                }
                            ) {}
                            .padding(vertical = 1.dp)
                            .heightIn(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }

            }
        }
    }
}
