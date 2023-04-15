package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.HomepageScaffoldState
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.TrainPartGraph
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */

@Composable
fun TrainStaticPage() {
    val viewModel: TrainPartViewModel = hiltViewModel()
    val part = viewModel.homeTrainPartStatic.collectAsState(initial = HomeTrainPartPage()).value
    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    val title = stringResource(id = R.string.title_train_part)
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            HomepageScaffoldState(
                title = title,
            )
        )
    }
    TrainStaticPage(homeTrainPartPage = part) {
        appScaffoldViewModel.onRoute(TrainPartGraph.TrainPartDetailNavItem.getRoute(it.trainPart))
    }
}

@Composable
fun TrainStaticPage(
    homeTrainPartPage: HomeTrainPartPage,
    onPartClick: (TrainPartStaticPage) -> Unit = {},
) {
    val scaffoldProperty = LocalScaffoldProperty.current
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
