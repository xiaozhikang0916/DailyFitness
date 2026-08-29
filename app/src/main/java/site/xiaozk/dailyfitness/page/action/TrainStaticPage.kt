package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.TrainPartGraph
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import site.xiaozk.dailyfitness.widget.HomePageScaffold
import site.xiaozk.dailyfitness.widget.ScaffoldProperty

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */

@Composable
fun TrainStaticPage() {
    val viewModel: TrainPartViewModel = hiltViewModel()
    val part = viewModel.homeTrainPartStatic.collectAsState(initial = HomeTrainPartPage()).value
    val navController = LocalNavController.current
    val title = stringResource(id = R.string.title_train_part)
    HomePageScaffold(title = title) { scaffoldProperty ->
        TrainStaticPage(
            homeTrainPartPage = part,
            onPartClick = {
                navController.navigate(TrainPartGraph.TrainPartDetailNavItem.getRoute(it.trainPart))
            },
            scaffoldProperty = scaffoldProperty,
        )
    }
}

@Composable
fun TrainStaticPage(
    homeTrainPartPage: HomeTrainPartPage,
    onPartClick: (TrainPartStaticPage) -> Unit = {},
    scaffoldProperty: ScaffoldProperty = ScaffoldProperty(),
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scaffoldProperty.scrollConnection)
            .padding(all = 12.dp),
        contentPadding = scaffoldProperty.padding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HomeTrainPartHeadCard(homeTrainPartPage)
        }
        items(homeTrainPartPage.parts) {
            TrainPartCard(
                trainPartStaticPage = it,
                modifier = Modifier
                    .fillMaxWidth()
            ) { onPartClick(it) }
        }
    }
}
