@file:JvmName("TrainPartPageKt")

package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import site.xiaozk.dailyfitness.utils.getLocalDateFormatter

/**
 * @author: xiaozhikang
 * @create: 2023/3/22
 */
@Composable
fun TrainPartPage() {
    val viewModel: TrainPartViewModel = hiltViewModel()
    val part = viewModel.trainPartStatic.collectAsState(initial = TrainPartStaticPage()).value
    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    if (part == null) {
        SideEffect {
            appScaffoldViewModel.back()
        }
    } else {
        val title = stringResource(id = R.string.title_train_part)
        val actionEditDesc = stringResource(R.string.action_desc_edit_train_part)
        val actionAddDesc = stringResource(R.string.action_desc_add_action)
        LaunchedEffect(key1 = part) {
            appScaffoldViewModel.scaffoldState.emit(
                SubpageScaffoldState(
                    title = title,
                    actionItems = listOf(
                        TopAction.iconRouteAction(
                            icon = Icons.Default.Add,
                            actionDesc = actionAddDesc,
                            route = Route(TrainPartGraph.AddTrainActionNavItem.getRoute(part.trainPart)),
                        ),
                        TopAction.iconRouteAction(
                            icon = Icons.Default.Edit,
                            actionDesc = actionEditDesc,
                            route = Route(TrainPartGraph.AddTrainPartNavItem.getRoute(part.trainPart)),
                        )
                    )
                )
            )
        }
        TrainPartPage(trainPartStaticPage = part) {
            appScaffoldViewModel.onRoute(TrainPartGraph.TrainActionDetailNavItem.getRoute(it.action))
        }
    }
}

@Composable
fun TrainPartPage(
    trainPartStaticPage: TrainPartStaticPage,
    onTrainActionClick: (TrainActionStaticPage) -> Unit = {},
) {
    val scaffoldProperty = LocalScaffoldProperty.current
    val dateFormatter = remember {
        getLocalDateFormatter()
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .nestedScroll(scaffoldProperty.scrollConnection),
        contentPadding = scaffoldProperty.padding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TrainPartCard(
                trainPartStaticPage = trainPartStaticPage,
                modifier = Modifier.fillMaxWidth(),
                isHead = true,
            )
        }
        items(trainPartStaticPage.actions) {
            TrainActionCard(
                actionPage = it,
                modifier = Modifier
                    .fillMaxWidth(),
                dateFormatter = dateFormatter,
            ) { onTrainActionClick(it) }
        }
    }
}