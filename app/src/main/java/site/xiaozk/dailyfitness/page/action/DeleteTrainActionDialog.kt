package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainAction
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @create: 2023/4/8
 */

@Composable
fun DeleteTrainActionDialog() {
    val viewModel: DeleteTrainActionViewModel = hiltViewModel()
    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    val state = viewModel.flow.collectAsState()
    LaunchedEffect(key1 = state.value.deleteStatus) {
        if (state.value.deleteStatus == ActionStatus.Done) {
            appScaffoldViewModel.showSnackbarAndBack("删除成功")
        } else if (state.value.deleteStatus is ActionStatus.Failed) {
            appScaffoldViewModel.showSnackbar("删除失败")
        }
    }
    LaunchedEffect(key1 = state.value.loadStatus) {
        if (state.value.loadStatus is ActionStatus.Failed) {
            appScaffoldViewModel.showSnackbarAndBack("加载成功")
        }
    }

    val dismiss = rememberUpdatedState(newValue = {
        appScaffoldViewModel.back()
    })

    val action = state.value.action
    if (action.id > 0) {
        AlertDialog(
            onDismissRequest = dismiss.value,
            confirmButton = {
                Text(
                    text = "删除",
                    modifier = Modifier
                        .clickable {
                            viewModel.performDelete()
                        },
                    textAlign = TextAlign.Center
                )
            },
            dismissButton = {
                Text(
                    text = "取消",
                    modifier = Modifier
                        .clickable { dismiss.value() },
                    textAlign = TextAlign.Center
                )
            },
            title = {
                Text(text = "删除训练动作")
            },
            text = {
                Text(
                    text = "你将要删除训练动作${action.actionName}。删除前请保证此训练动作没有训练记录。"
                )
            }
        )
    }
}

@HiltViewModel
class DeleteTrainActionViewModel @Inject constructor(
    private val trainRepo: ITrainActionRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val id: Int
        get() = savedStateHandle["actionId"] ?: 0
    private val _flow: MutableStateFlow<DeleteActionState> = MutableStateFlow(DeleteActionState())
    val flow = _flow.asStateFlow()

    init {
        viewModelScope.launch {
            _flow.emit(DeleteActionState(loadStatus = ActionStatus.Loading))
            try {
                _flow.emit(
                    DeleteActionState(
                        action = trainRepo.getAction(id).first(),
                        loadStatus = ActionStatus.Done,
                    )
                )
            } catch (e: Exception) {
                _flow.emit(DeleteActionState(loadStatus = ActionStatus.Failed(e)))
            }
        }
    }


    fun performDelete() {
        viewModelScope.launch {
            val current = _flow.value
            _flow.value = current.copy(deleteStatus = ActionStatus.Loading)
            try {
                trainRepo.removeTrainAction(current.action)
                _flow.value = current.copy(deleteStatus = ActionStatus.Done)
            } catch (e: Exception) {
                _flow.value = current.copy(deleteStatus = ActionStatus.Failed(e))
            }
        }
    }
}

data class DeleteActionState(
    val action: TrainAction = TrainAction(),
    val loadStatus: ActionStatus = ActionStatus.Idle,
    val deleteStatus: ActionStatus = ActionStatus.Idle,
)