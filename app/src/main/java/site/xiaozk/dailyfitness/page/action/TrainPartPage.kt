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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.Route
import site.xiaozk.dailyfitness.nav.SubpageScaffoldState
import site.xiaozk.dailyfitness.nav.TopAction
import site.xiaozk.dailyfitness.nav.TrainPartGraph
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage

/**
 * @author: xiaozhikang
 * @create: 2023/3/22
 */
@Composable
fun TrainPartPage() {
    val viewModel: TrainPartViewModel = hiltViewModel()
    val part = viewModel.trainPartStatic.collectAsState(initial = TrainPartStaticPage()).value
    val appScaffoldViewModel: AppScaffoldViewModel = hiltViewModel()
    LaunchedEffect(key1 = part) {
        appScaffoldViewModel.scaffoldState.emit(
            SubpageScaffoldState(
                title = "训练动作",
                actionItems = listOf(
                    TopAction.iconRouteAction(
                        icon = Icons.Default.Add,
                        actionDesc = "add new train action",
                        route = Route(TrainPartGraph.AddTrainActionNavItem.getRoute(part.trainPart)),
                    )
                )
            )
        )
    }
    TrainPartPage(trainPartStaticPage = part) {
        appScaffoldViewModel.onRoute(TrainPartGraph.TrainActionDetailNavItem.getRoute(it.action))
    }
}

@Composable
fun TrainPartPage(
    trainPartStaticPage: TrainPartStaticPage,
    onTrainActionClick: (TrainActionStaticPage) -> Unit = {},
) {
    val scaffoldProperty = LocalScaffoldProperty.current
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
                    .fillMaxWidth()
            ) { onTrainActionClick(it) }
        }
    }
}