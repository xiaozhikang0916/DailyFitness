package site.xiaozk.dailyfitness.page.action.parts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.TrainPartGraph
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.widget.BackButton
import site.xiaozk.dailyfitness.widget.FloatingActionButtonShowHide
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainPartDetail(trainPartId: Int) {
    val viewModel: TrainPartDetailViewModel = hiltViewModel()
    val trainPart = viewModel.getPartDetail(trainPartId).collectAsState(TrainPartGroup()).value
    val nav = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(title = { Text(text = "训练部位") }, navigationIcon = { BackButton() })
        },
        floatingActionButton = {
            FloatingActionButtonShowHide(
                onClick = { nav?.navigate(TrainPartGraph.AddTrainActionNavItem.getRoute(trainPart.part)) },
                topAppBarState = scrollBehavior.state
            ) {
                Icon(painter = rememberVectorPainter(image = Icons.Default.Add), contentDescription = "add new train action")
            }
        }
    ) { padding ->
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
                    TrainActionInPart(action = it)
                }
            }
        }
    }
}

@Composable
private fun TrainActionInPart(action: TrainAction) {
    Card(
        modifier = Modifier
            .padding(all = 4.dp)
            .fillMaxWidth()
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