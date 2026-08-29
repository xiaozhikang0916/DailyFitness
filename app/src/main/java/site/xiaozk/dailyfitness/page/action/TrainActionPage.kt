package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.TrainPartGraph
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.utils.getLocalDateFormatter
import site.xiaozk.dailyfitness.utils.getLocalDateTimeFormatter
import site.xiaozk.dailyfitness.widget.ScaffoldProperty
import site.xiaozk.dailyfitness.widget.SubPageScaffold

/**
 * @author: xiaozhikang
 * @create: 2023/3/22
 */

@Composable
fun TrainActionPage() {
    val viewModel: TrainPartViewModel = hiltViewModel()
    val actionState = viewModel.trainActionStatic.collectAsState(initial = TrainActionStaticPage()).value
    val navController = LocalNavController.current

    if (actionState == null) {
        SideEffect {
            navController.popBackStack()
        }
    } else {
        val title = stringResource(id = R.string.title_train_action)
        val actionEditDesc = stringResource(R.string.action_desc_edit_train_action)
        val actionDeleteDesc = stringResource(R.string.action_desc_delete_train_action)
        SubPageScaffold(
            title = title,
            onBack = { navController.popBackStack() },
            actions = {
                IconButton(
                    onClick = {
                        navController.navigate(TrainPartGraph.AddTrainActionNavItem.getRoute(actionState.action))
                    }
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = actionEditDesc)
                }
                IconButton(
                    onClick = {
                        navController.navigate(TrainPartGraph.DeleteTrainActionNavItem.getRoute(actionState.action))
                    }
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = actionDeleteDesc)
                }
            }
        ) { scaffoldProperty ->
            TrainActionPage(
                actionStaticPage = actionState,
                onWorkoutLongClick = {
                    navController.navigate(
                        TrainingDayGroup.DeleteWorkoutNavItem.getRoute(it.id)
                    )
                },
                scaffoldProperty = scaffoldProperty,
            )
        }
    }
}

@Composable
fun TrainActionPage(
    actionStaticPage: TrainActionStaticPage,
    onWorkoutLongClick: (DailyWorkoutAction) -> Unit = {},
    scaffoldProperty: ScaffoldProperty = ScaffoldProperty(),
) {
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
                HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}