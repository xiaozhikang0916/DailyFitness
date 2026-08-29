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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.nav.LocalNavBackStack
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import site.xiaozk.dailyfitness.nav.AddTrainAction
import site.xiaozk.dailyfitness.nav.TrainActionDetail
import site.xiaozk.dailyfitness.page.action.parts.AddTrainPartPage
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
fun TrainPartPage(partId: Int) {
    val viewModel = hiltViewModel<TrainPartViewModel, TrainPartViewModel.Factory>(
        creationCallback = { it.create(partId = partId, actionId = -1) }
    )
    val part = viewModel.trainPartStatic.collectAsState(initial = TrainPartStaticPage()).value
    val navBackStack = LocalNavBackStack.current
    val systemBack = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var addTrainPartId by remember { mutableStateOf<Int?>(null) }
    if (part == null) {
        SideEffect {
            systemBack?.onBackPressed()
        }
    } else {
        val title = stringResource(id = R.string.title_train_part)
        val actionEditDesc = stringResource(R.string.action_desc_edit_train_part)
        val actionAddDesc = stringResource(R.string.action_desc_add_action)
        SubPageScaffold(
            title = title,
            onBack = { systemBack?.onBackPressed() },
            actions = {
                IconButton(
                    onClick = {
                        navBackStack.add(
                            AddTrainAction(partId = part.trainPart.id, actionId = 0)
                        )
                    }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = actionAddDesc)
                }
                IconButton(
                    onClick = {
                        addTrainPartId = part.trainPart.id
                    }
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = actionEditDesc)
                }
            }
        ) { scaffoldProperty ->
            TrainPartPage(
                trainPartStaticPage = part,
                onTrainActionClick = {
                    navBackStack.add(
                        TrainActionDetail(actionId = it.action.id)
                    )
                },
                scaffoldProperty = scaffoldProperty,
            )
        }
        addTrainPartId?.let { id ->
            AddTrainPartPage(
                partId = id,
                onDismiss = { addTrainPartId = null },
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