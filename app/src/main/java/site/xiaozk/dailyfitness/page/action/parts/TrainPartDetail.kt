package site.xiaozk.dailyfitness.page.action.parts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.Route
import site.xiaozk.dailyfitness.nav.SubpageScaffoldState
import site.xiaozk.dailyfitness.nav.TopAction
import site.xiaozk.dailyfitness.nav.TrainPartGraph
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainPartDetail(trainPartId: Int, padding: PaddingValues = PaddingValues()) {
    val viewModel: TrainPartDetailViewModel = hiltViewModel()
    val trainPart = viewModel.getPartDetail(trainPartId).collectAsState(TrainPartGroup()).value

    val appScaffoldViewModel: AppScaffoldViewModel = hiltViewModel()
    LaunchedEffect(key1 = trainPart) {
        appScaffoldViewModel.scaffoldState.emit(
            SubpageScaffoldState(
                title = "训练部位",
                actionItems = listOf(
                    TopAction.iconRouteAction(
                        icon = Icons.Default.Add,
                        actionDesc = "add new train action",
                        route = Route(TrainPartGraph.AddTrainActionNavItem.getRoute(trainPart.part)),
                    )
                )
            )
        )
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    if (trainPart.part.id != 0) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            item {
                Text(
                    text = trainPart.part.partName,
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .fillMaxWidth()
                )
            }
            items(trainPart.actions) {
                TrainActionInPart(action = it) { ac ->
                    appScaffoldViewModel.onRoute(TrainPartGraph.AddTrainActionNavItem.getRoute(ac.part, ac))
                }
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrainActionInPart(action: TrainActionWithPart, onActionLongClick: (TrainActionWithPart) -> Unit) {
    Card(
        modifier = Modifier
            .padding(all = 4.dp)
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = {
                    onActionLongClick(action)
                }
            ) { }
    ) {
        Text(text = action.actionName)
    }
}

@HiltViewModel
class TrainPartDetailViewModel @Inject constructor(
    private val repo: ITrainActionRepository,
) : ViewModel() {
    fun getPartDetail(id: Int): Flow<TrainPartGroup> {
        return repo.getActionsOfPart(id)
    }
}