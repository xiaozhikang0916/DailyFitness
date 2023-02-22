@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.action.parts

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.widget.BackButton
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/28
 */


@Composable
fun AddTrainActionPage(partId: Int) {
    val viewModel: AddTrainActionViewModel = hiltViewModel()
    viewModel.initData(partId)
    val nav = LocalNavController.current
    val state = viewModel.state.collectAsState().value
    val part = state.part
    val status = state.status
    var isTimed by remember {
        mutableStateOf(false)
    }
    var isCounted by remember {
        mutableStateOf(false)
    }
    var isWeighted by remember {
        mutableStateOf(false)
    }
    var name by remember {
        mutableStateOf("")
    }
    LaunchedEffect(key1 = status) {
        if (status == ActionStatus.Done) {
            nav?.popBackStack()
        }
    }
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(title = { Text(text = "新增训练动作") }, navigationIcon = {
                BackButton()
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = part.partName,
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Checkbox(checked = isTimed, onCheckedChange = { isTimed = it })
                Text(text = "计时动作")
            }

            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Checkbox(checked = isWeighted, onCheckedChange = { isWeighted = it })
                Text(text = "计重动作")
            }

            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Checkbox(checked = isCounted, onCheckedChange = { isCounted = it })
                Text(text = "计次动作")
            }

            Button(
                onClick = {
                    viewModel.addTrainAction(
                        part = part,
                        actionName = name,
                        isTimed = isTimed,
                        isWeighted = isWeighted,
                        isCounted = isCounted
                    )
                },
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "Submit")
            }
        }
    }
}

@HiltViewModel
class AddTrainActionViewModel @Inject constructor(
    private val repo: ITrainActionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AddTrainActionState())
    val state = _state.asStateFlow()

    fun initData(partId: Int) {
        viewModelScope.launch {
            repo.getActionsOfPart(partId).collect {
                _state.emit(
                    _state.value.copy(part = it.part)
                )
            }
        }
    }

    fun addTrainAction(
        part: TrainPart,
        actionName: String,
        isTimed: Boolean,
        isWeighted: Boolean,
        isCounted: Boolean,
    ) {
        viewModelScope.launch {
            _state.emit(_state.value.copy(status = ActionStatus.Loading))
            try {
                repo.addTrainAction(
                    TrainAction(
                        part = part,
                        actionName = actionName,
                        isTimedAction = isTimed,
                        isWeightedAction = isWeighted,
                        isCountedAction = isCounted,
                    )
                )
                _state.emit(_state.value.copy(status = ActionStatus.Done))
            } catch (e: Exception) {
                Log.e("AddTrainAction", "Add action failed", e)
                _state.emit(_state.value.copy(status = ActionStatus.Failed(e)))
            }
        }
    }
}

data class AddTrainActionState(
    val part: TrainPart = TrainPart(),
    val status: ActionStatus = ActionStatus.Idle,
)