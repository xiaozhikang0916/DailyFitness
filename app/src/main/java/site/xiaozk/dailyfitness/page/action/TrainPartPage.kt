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
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import site.xiaozk.dailyfitness.utils.getLocalDateFormatter
import site.xiaozk.dailyfitness.widget.ScaffoldProperty
import site.xiaozk.dailyfitness.widget.SubPageScaffold

/**
 * @author: xiaozhikang
 * @create: 2023/3/22
 */
@Composable
fun TrainPartPage() {
    val viewModel: TrainPartViewModel = hiltViewModel()
    val part = viewModel.trainPartStatic.collectAsState(initial = TrainPartStaticPage()).value
    val navController = LocalNavController.current
    if (part == null) {
        SideEffect {
            navController.popBackStack()
        }
    } else {
        val title = stringResource(id = R.string.title_train_part)
        val actionEditDesc = stringResource(R.string.action_desc_edit_train_part)
        val actionAddDesc = stringResource(R.string.action_desc_add_action)
        SubPageScaffold(
            title = title,
            onBack = { navController.popBackStack() },
            actions = {
                IconButton(
                    onClick = {
                        navController.navigate(TrainPartGraph.AddTrainActionNavItem.getRoute(part.trainPart))
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = actionAddDesc)
                }
                IconButton(
                    onClick = {
                        navController.navigate(TrainPartGraph.AddTrainPartNavItem.getRoute(part.trainPart))
                    }
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = actionEditDesc)
                }
            }
        ) { scaffoldProperty ->
            TrainPartPage(
                trainPartStaticPage = part,
                onTrainActionClick = {
                    navController.navigate(TrainPartGraph.TrainActionDetailNavItem.getRoute(it.action))
                },
                scaffoldProperty = scaffoldProperty,
            )
        }
    }
}

@Composable
fun TrainPartPage(
    trainPartStaticPage: TrainPartStaticPage,
    onTrainActionClick: (TrainActionStaticPage) -> Unit = {},
    scaffoldProperty: ScaffoldProperty = ScaffoldProperty(),
) {
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