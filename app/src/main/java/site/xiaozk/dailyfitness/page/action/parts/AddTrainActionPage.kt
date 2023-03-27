@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.action.parts

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.FullDialogScaffoldState
import site.xiaozk.dailyfitness.nav.PageHandleAction
import site.xiaozk.dailyfitness.nav.PageHandleType
import site.xiaozk.dailyfitness.nav.TopAction
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPart
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/28
 */
// todo mvi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainActionPage(partId: Int, actionId: Int = 0, padding: PaddingValues = PaddingValues()) {
    val viewModel: AddTrainActionViewModel = hiltViewModel()
    viewModel.initData(partId, actionId)
    val state = viewModel.state.collectAsState().value
    val part = state?.part
    val status = state?.status
    var isTimed by remember(state?.action) {
        mutableStateOf(state?.action?.isTimedAction ?: false)
    }
    var isCounted by remember(state?.action) {
        mutableStateOf(state?.action?.isCountedAction ?: false)
    }
    var isWeighted by remember(state?.action) {
        mutableStateOf(state?.action?.isWeightedAction ?: false)
    }
    var name by remember(state?.action) {
        mutableStateOf(state?.action?.actionName ?: "")
    }
    val inputValid = remember(name, part) {
        derivedStateOf {
            name.isNotBlank() && part != null
        }
    }
    val appScaffoldViewModel: AppScaffoldViewModel = hiltViewModel()
    LaunchedEffect(key1 = inputValid.value) {
        appScaffoldViewModel.scaffoldState.emit(
            FullDialogScaffoldState(
                title = "新增训练动作",
                actionItems = listOf(
                    TopAction.textPageAction(
                        text = "SAVE",
                        type = PageHandleType.SAVE,
                        valid = inputValid.value,
                    )
                )
            )
        )
    }
    LaunchedEffect(key1 = status) {
        if (status == ActionStatus.Done) {
            appScaffoldViewModel.showSnackbarAndBack("添加成功")
        } else if (status is ActionStatus.Failed) {
            appScaffoldViewModel.showSnackbar("添加失败")
        }
    }
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.topAction.collect {
            if (it.actionType is PageHandleAction && it.actionType.type == PageHandleType.SAVE) {
                viewModel.addTrainAction(
                    actionName = name,
                    isTimed = isTimed,
                    isWeighted = isWeighted,
                    isCounted = isCounted
                )
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 12.dp)
    ) {

        OutlinedTextField(
            value = part?.partName ?: "",
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth(),
            readOnly = true,
            label = {
                Text(text = "训练部位")
            }
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier
                .fillMaxWidth(),
            label = {
                Text(text = "动作名称")
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterChip(
                selected = isTimed,
                onClick = { isTimed = isTimed.not() },
                label = { Text(text = "计时动作") },
                leadingIcon = {
                    if (isTimed) {
                        CheckedIcon()
                    }
                }
            )
            FilterChip(
                selected = isWeighted,
                onClick = { isWeighted = isWeighted.not() },
                label = { Text(text = "计重动作") },
                leadingIcon = {
                    if (isWeighted) {
                        CheckedIcon()
                    }
                }
            )
            FilterChip(
                selected = isCounted,
                onClick = { isCounted = isCounted.not() },
                label = { Text(text = "计次动作") },
                leadingIcon = {
                    if (isCounted) {
                        CheckedIcon()
                    }
                }
            )

        }
    }
}

@Composable
private fun CheckedIcon() {
    Icon(
        painter = rememberVectorPainter(image = Icons.Default.Done),
        modifier = Modifier.size(18.dp),
        contentDescription = null,
    )

}

@HiltViewModel
class AddTrainActionViewModel @Inject constructor(
    private val repo: ITrainActionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<AddTrainActionState?>(null)
    val state = _state.asStateFlow()

    fun initData(partId: Int, actionId: Int = 0) {
        Log.i("AddTrainAction", "Load action with part id $partId, action id $actionId")
        viewModelScope.launch {
            _state.emitAll(
                repo.getActionsOfPart(partId).map {
                    if (actionId != 0) {
                        it.actions.find { action -> action.id == actionId }
                    } else {
                        null
                    } ?: TrainActionWithPart(part = it.part)
                }.map {
                    AddTrainActionState(it.action, it.part)
                }
            )
        }
    }

    fun addTrainAction(
        actionName: String,
        isTimed: Boolean,
        isWeighted: Boolean,
        isCounted: Boolean,
    ) {
        val current = _state.value
        if (current != null) {
            viewModelScope.launch {
                _state.emit(current.copy(status = ActionStatus.Loading))
                try {
                    repo.addOrUpdateTrainAction(
                        current.action.copy(
                            actionName = actionName,
                            isTimedAction = isTimed,
                            isWeightedAction = isWeighted,
                            isCountedAction = isCounted,
                        )
                    )
                    _state.emit(current.copy(status = ActionStatus.Done))
                } catch (e: Exception) {
                    Log.e("AddTrainAction", "Add action failed", e)
                    _state.emit(current.copy(status = ActionStatus.Failed(e)))
                }
            }
        }
    }
}

data class AddTrainActionState(
    val action: TrainAction,
    val part: TrainPart,
    val status: ActionStatus = ActionStatus.Idle,
)