package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.Route
import site.xiaozk.dailyfitness.nav.SubpageScaffoldState
import site.xiaozk.dailyfitness.nav.TopAction
import site.xiaozk.dailyfitness.nav.TrainPartGraph
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.utils.getLocalDateFormatter
import site.xiaozk.dailyfitness.utils.getLocalDateTimeFormatter

/**
 * @author: xiaozhikang
 * @create: 2023/3/22
 */

@Composable
fun TrainActionPage() {
    val viewModel: TrainPartViewModel = hiltViewModel()
    val actionState = viewModel.trainActionStatic.collectAsState(initial = TrainActionStaticPage()).value
    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()

    if (actionState == null) {
        SideEffect {
            appScaffoldViewModel.back()
        }
    } else {
        val title = stringResource(id = R.string.title_train_action)
        val actionEditDesc = stringResource(R.string.action_desc_edit_train_action)
        val actionDeleteDesc = stringResource(R.string.action_desc_delete_train_action)
        LaunchedEffect(key1 = actionState) {
            appScaffoldViewModel.scaffoldState.emit(
                SubpageScaffoldState(
                    title = title,
                    actionItems = listOf(
                        TopAction.iconRouteAction(
                            icon = Icons.Default.Edit,
                            actionDesc = actionEditDesc,
                            route = Route(TrainPartGraph.AddTrainActionNavItem.getRoute(actionState.action)),
                        ),
                        TopAction.iconRouteAction(
                            icon = Icons.Default.Delete,
                            actionDesc = actionDeleteDesc,
                            route = Route(TrainPartGraph.DeleteTrainActionNavItem.getRoute(actionState.action)),
                        )
                    )
                )
            )
        }
        TrainActionPage(
            actionStaticPage = actionState
        ) {
            appScaffoldViewModel.onRoute(
                TrainingDayGroup.DeleteWorkoutNavItem.getRoute(it.id)
            )
        }
    }
}

@Composable
fun TrainActionPage(
    actionStaticPage: TrainActionStaticPage,
    onWorkoutLongClick: (DailyWorkoutAction) -> Unit = {},
) {
    val scaffoldProperty = LocalScaffoldProperty.current
    val dateTimeFormatter = remember {
        getLocalDateTimeFormatter()
    }
    val dateFormatter = remember {
        getLocalDateFormatter()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .nestedScroll(scaffoldProperty.scrollConnection),
        contentPadding = scaffoldProperty.padding,
    ) {
        item {
            TrainActionCard(
                actionPage = actionStaticPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                isHead = true,
                dateFormatter = dateFormatter,
            )
        }
        itemsIndexed(actionStaticPage.workouts) { index, workout ->
            TrainActionWorkoutCard(
                workout = workout,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                formatter = dateTimeFormatter,
                onCardLongClick = onWorkoutLongClick,
            )
            if (index < actionStaticPage.workoutCount - 1) {
                Divider(modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}