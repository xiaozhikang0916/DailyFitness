package site.xiaozk.dailyfitness.page.training

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.Route
import site.xiaozk.dailyfitness.nav.SubpageScaffoldState
import site.xiaozk.dailyfitness.nav.TopAction
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListActionPair
import site.xiaozk.dailyfitness.utils.getLocalDateFormatter
import java.time.ZoneId

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/25
 */
@Composable
fun TrainingDayDetailPage() {
    val viewModel: TrainingDayDetailViewModel = hiltViewModel()
    val data = viewModel.trainingData

    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            SubpageScaffoldState(
                title = "训练日志",
                actionItems = listOf(
                    TopAction.iconRouteAction(
                        Icons.Default.Add,
                        "添加训练动作",
                        Route(TrainingDayGroup.TrainDayAddActionNavItem.route)
                    )
                )
            )
        )
    }
    TrainingDayDetail(
        data = data.collectAsState(initial = null).value ?: DailyWorkout(viewModel.date),
        onTrainingActionDeleted = {
            appScaffoldViewModel.onRoute(
                TrainingDayGroup.DeleteWorkoutNavItem.getRoute(it.id)
            )
        }
    )
}

@Composable
fun TrainingDayDetail(
    data: DailyWorkout,
    modifier: Modifier = Modifier,
    onTrainingActionDeleted: (DailyWorkoutAction) -> Unit = {},
) {
    val dateFormat = remember {
        getLocalDateFormatter().withZone(ZoneId.systemDefault())
    }
    val scaffoldProperty = LocalScaffoldProperty.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .nestedScroll(scaffoldProperty.scrollConnection),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(
            top = scaffoldProperty.padding.calculateTopPadding(),
            bottom = scaffoldProperty.padding.calculateBottomPadding() + 12.dp
        ),
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
        items(data.actions, key = ::key) {
            DailyTrainingAction(action = it, onActionDelete = onTrainingActionDeleted)
        }
    }
}

private fun key(item: DailyWorkoutListActionPair): String {
    return item.action.id.toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DailyTrainingAction(
    action: DailyWorkoutListActionPair,
    onActionDelete: (DailyWorkoutAction) -> Unit,
) {
    var expended by rememberSaveable(key(action)) {
        mutableStateOf(true)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = MaterialTheme.shapes.medium)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                .clickable { expended = !expended }
                .padding(all = 12.dp)
        ) {
            Text(
                text = action.action.actionName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${action.trainAction.size}组",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(expended) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp)
            ) {
                action.trainAction.forEach { action ->
                    val string = buildAnnotatedString {
                        append(action.displayText.joinToString(separator = " "))
                        if (action.note.isNotBlank()) {
                            append('\n')
                            pushStyle(MaterialTheme.typography.bodySmall.toSpanStyle())
                            append(action.note)
                            pop()
                        }
                    }
                    Text(
                        text = string,
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = {
                                    onActionDelete(action)
                                }
                            ) {}
                            .padding(vertical = 1.dp, horizontal = 12.dp)
                            .fillMaxWidth()
                            .heightIn(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
