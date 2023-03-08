package site.xiaozk.dailyfitness.page.training

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.repository.model.DailyTrainAction
import site.xiaozk.dailyfitness.repository.model.DailyTrainingPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainingDayData
import site.xiaozk.dailyfitness.widget.BackButton
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
                        Icon(painter = rememberVectorPainter(image = Icons.Default.Create), contentDescription = "add")
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

        if (deleteActionDialog != null) {
            val dismiss = {
                deleteActionDialog = null
            }
            AlertDialog(
                onDismissRequest = dismiss,
                buttons = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            text = "取消",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { dismiss() },
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                        )
                        Text(
                            text = "删除",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    deleteActionDialog?.let {
                                        viewModel.removeTrainAction(it)
                                    }
                                    dismiss()
                                },
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                title = {
                    Text(text = "确认删除？")
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
            DailyTrainingPart(group = it, onActionDelete = onTrainingActionDeleted)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DailyTrainingPart(group: DailyTrainingPartGroup, onActionDelete: (DailyTrainAction) -> Unit) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                group.trainAction.forEach { action ->
                    Text(
                        text = action.displayText.joinToString(separator = " "),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .combinedClickable(
                                onLongClick = {
                                    onActionDelete(action)
                                }
                            ) {}

                    )
                }
            }
        }
    }
}