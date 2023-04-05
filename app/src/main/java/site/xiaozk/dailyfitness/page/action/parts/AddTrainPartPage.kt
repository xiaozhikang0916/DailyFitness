@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.action.parts

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
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
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainPart
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainPartPage() {
    val viewModel: AddTrainPartViewModel = hiltViewModel()
    val state = viewModel.status.collectAsState()
    val appScaffoldViewModel: AppScaffoldViewModel = hiltViewModel()
    LaunchedEffect(key1 = state.value) {
        if (state.value.submitStatus == ActionStatus.Done) {
             appScaffoldViewModel.showSnackbarAndBack("添加成功")
        } else if (state.value.submitStatus is ActionStatus.Failed) {
            appScaffoldViewModel.showSnackbar("添加失败")
        }
    }
    var name by remember(state.value.part) {
        mutableStateOf(state.value.part.partName)
    }
    AlertDialog(
        onDismissRequest = {
            appScaffoldViewModel.back()
        },
        confirmButton = {
            TextButton(onClick = { viewModel.addPart(name) }) {
                Text(text = "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                appScaffoldViewModel.back()
            }) {
                Text(text = "取消")
            }
        },
        title = { Text(text = "新增训练部位") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth()
            )
        },
    )
}

@HiltViewModel
class AddTrainPartViewModel @Inject constructor(
    private val repo: ITrainActionRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val partId: Int
        get() = savedStateHandle["partId"] ?: -1
    private val _status = MutableStateFlow(AddTrainPartState())
    val status = _status.asStateFlow()

    init {
        Log.i("AddTrainPart", "Load part with part id $partId")
        viewModelScope.launch {
            _status.emitAll(
                repo.getActionsOfPart(partId).map {
                    AddTrainPartState(
                        it.part,
                    )
                }
            )
        }
    }

    fun addPart(name: String) {
        viewModelScope.launch {
            val current = status.value
            _status.emit(current.copy(submitStatus = ActionStatus.Loading))
            try {
                repo.addOrUpdateTrainPart(current.part.copy(partName = name))
                _status.emit(current.copy(submitStatus = ActionStatus.Done))
            } catch (e: Exception) {
                Log.e("AddTrainPart", "Add part failed", e)
                _status.emit(current.copy(submitStatus = ActionStatus.Failed(e)))
            }
        }
    }
}

data class AddTrainPartState(
    val part: TrainPart = TrainPart(),
    val submitStatus: ActionStatus = ActionStatus.Idle,
)