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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.repository.model.DailyTrainAction
import site.xiaozk.dailyfitness.repository.model.DailyTrainingActionList
import site.xiaozk.dailyfitness.repository.model.TrainingDayData
import site.xiaozk.dailyfitness.widget.BackButton
import java.time.LocalDate
import java.time.ZoneId
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val nav = LocalNavController.current
    var deleteActionDialog by remember {
        mutableStateOf<DailyTrainAction?>(null)
    }
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "训练日志") },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = {
                            nav?.navigate(TrainingDayGroup.TrainDayAddActionNavItem.route)
                        }
                    ) {
                        Icon(painter = rememberVectorPainter(image = Icons.Default.Add), contentDescription = "add")
                    }
                }
            )
        },
    ) {
        TrainingDayDetail(
            data = data.collectAsState(initial = null).value ?: TrainingDayData(date),
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            padding = it,
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
}

@Composable
fun TrainingDayDetail(
    data: TrainingDayData,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
    onTrainingActionDeleted: (DailyTrainAction) -> Unit = {},
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
private fun DailyTrainingAction(action: DailyTrainingActionList, onActionDelete: (DailyTrainAction) -> Unit) {
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
