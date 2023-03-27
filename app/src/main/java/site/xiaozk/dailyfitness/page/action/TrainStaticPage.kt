package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.HomepageScaffoldState
import site.xiaozk.dailyfitness.nav.TrainPartGraph
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */

@Composable
fun TrainPartPage() {
    val viewModel: TrainPartViewModel = hiltViewModel()
    val part = viewModel.trainParts.collectAsState(initial = TrainPartPage()).value
    val appScaffoldViewModel: AppScaffoldViewModel = hiltViewModel()
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            HomepageScaffoldState(
                title = "训练动作",
            )
        )
    }
    TrainPartPage(
        part, onCardClick = {
            appScaffoldViewModel.onRoute(TrainPartGraph.TrainPartDetailNavItem.getRoute(it.part))
        }, onCardLongClick = {
            appScaffoldViewModel.onRoute(TrainPartGraph.AddTrainPartNavItem.getRoute(it.part))
        }
    )
}

@Composable
fun TrainPartPage(
    page: TrainPartPage,
    onCardClick: (TrainPartGroup) -> Unit,
    onCardLongClick: (TrainPartGroup) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(page.allParts) {
            TrainPart(part = it, onCardClick, onCardLongClick)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrainPart(
    part: TrainPartGroup,
    onCardClick: (TrainPartGroup) -> Unit,
    onCardLongClick: (TrainPartGroup) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 4.dp)
            .combinedClickable(
                onLongClick = {
                    onCardLongClick(part)
                }
            ) {
                onCardClick(part)
            }
    ) {
        Text(
            text = part.part.partName, style = MaterialTheme.typography.titleMedium
        )
        part.actions.forEach {
            Text(
                text = it.actionName,
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}

@Composable
fun TrainStaticPage(
    homeTrainPartPage: HomeTrainPartPage,
    onPartClick: (TrainPartStaticPage) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HomeTrainPartHeadCard(homeTrainPartPage)
        }
        items(homeTrainPartPage.parts) {
            TrainPartCard(trainPartStaticPage = it, modifier = Modifier
                .fillMaxWidth()
                .clickable { onPartClick(it) })
        }
    }
}
