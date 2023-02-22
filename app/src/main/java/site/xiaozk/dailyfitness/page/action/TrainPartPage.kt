package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.AppHomeRootNav
import site.xiaozk.dailyfitness.nav.AppScaffoldState
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.TrainPartGraph
import site.xiaozk.dailyfitness.nav.updateAppScaffoldState
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainPartPage

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */

@Composable
fun TrainPartPage() {
    val viewModel: TrainPartViewModel = hiltViewModel()
    val part = viewModel.trainParts.collectAsState(initial = TrainPartPage()).value
    val localNav = LocalNavController.current
    localNav?.navController?.let {
        AppHomeRootNav.AppHomePage.TrainPartNavItem.updateAppScaffoldState(navController = it, state = AppScaffoldState(
            fabRoute = TrainPartGraph.AddTrainPartNavItem.route,
            showBottomNavBar = true,
        ))
    }
    TrainPartPage(part) {
        localNav?.navigate(TrainPartGraph.TrainPartDetailNavItem.getRoute(it.part))
    }
}

@Composable
fun TrainPartPage(page: TrainPartPage, onCardClick: (TrainPartGroup) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(page.allParts) {
            TrainPart(part = it, onCardClick)
        }
    }
}

@Composable
private fun TrainPart(part: TrainPartGroup, onCardClick: (TrainPartGroup) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 4.dp)
            .clickable {
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