package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.DelFailedSnackbar
import site.xiaozk.dailyfitness.nav.DelSuccessSnackbar
import site.xiaozk.dailyfitness.nav.LoadFailedSnackbar
import site.xiaozk.dailyfitness.nav.LocalAppSnackbarHostState
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainAction
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @create: 2023/4/8
 */

@Composable
fun DeleteTrainActionDialog(
    actionId: Int,
    onDismiss: () -> Unit,
) {
    // 本地状态对话框：每次打开重新组合 → 新 key → 独立 VM 实例
    val vmKey = remember { "delete-train-action-$actionId-${UUID.randomUUID()}" }
    val viewModel = hiltViewModel<DeleteTrainActionViewModel, DeleteTrainActionViewModel.Factory>(
        key = vmKey,
        creationCallback = { it.create(actionId) }
    )
    val appSnackbarHostState = LocalAppSnackbarHostState.current
    val state = viewModel.flow.collectAsState()
    val dismiss = rememberUpdatedState(onDismiss)
    LaunchedEffect(key1 = state.value.deleteStatus) {
        if (state.value.deleteStatus == ActionStatus.Done) {
            appSnackbarHostState.showSnackbar(DelSuccessSnackbar)
            dismiss.value()
        } else if (state.value.deleteStatus is ActionStatus.Failed) {
            appSnackbarHostState.showSnackbar(DelFailedSnackbar)
        }
    }
    LaunchedEffect(key1 = state.value.loadStatus) {
        if (state.value.loadStatus is ActionStatus.Failed) {
            appSnackbarHostState.showSnackbar(LoadFailedSnackbar)
            dismiss.value()
        }
    }

    val action = state.value.action
    if (action.id > 0) {
        AlertDialog(
            onDismissRequest = dismiss.value,
            confirmButton = {
                Text(
                    text = stringResource(R.string.dialog_action_delete),
                    modifier = Modifier
                        .clickable {
                            viewModel.performDelete()
                        },
                    textAlign = TextAlign.Center
                )
            },
            dismissButton = {
                Text(
                    text = stringResource(id = R.string.dialog_action_cancel),
                    modifier = Modifier
                        .clickable { dismiss.value() },
                    textAlign = TextAlign.Center
                )
            },
            title = {
                Text(text = stringResource(R.string.title_delete_train_action))
            },
            text = {
                Text(
                    text = stringResource(R.string.desc_delete_train_action, action.actionName)
                )
            }
        )
    }
}

@HiltViewModel(assistedFactory = DeleteTrainActionViewModel.Factory::class)
class DeleteTrainActionViewModel @AssistedInject constructor(
    private val trainRepo: ITrainActionRepository,
    @Assisted private val actionId: Int,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(actionId: Int): DeleteTrainActionViewModel
    }

    private val _flow: MutableStateFlow<DeleteActionState> = MutableStateFlow(DeleteActionState())
    val flow = _flow.asStateFlow()

    init {
        viewModelScope.launch {
            _flow.emit(DeleteActionState(loadStatus = ActionStatus.Loading))
            try {
                _flow.emit(
                    DeleteActionState(
                        action = trainRepo.getAction(actionId).first(),
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