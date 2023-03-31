@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.action.parts

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.FullDialogScaffoldState
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.PageHandleAction
import site.xiaozk.dailyfitness.nav.PageHandleType
import site.xiaozk.dailyfitness.nav.TopAction
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPart
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/28
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainActionPage(partId: Int, actionId: Int = 0) {
    val viewModel: AddTrainActionViewModel = hiltViewModel()
    LaunchedEffect(key1 = Unit) {
        viewModel.initData(partId, actionId)
    }
    val state = viewModel.state.collectAsState().value
    val part = state.part
    val status = state.status
    val appScaffoldViewModel: AppScaffoldViewModel = hiltViewModel()
    LaunchedEffect(key1 = state.inputValid) {
        appScaffoldViewModel.scaffoldState.emit(
            FullDialogScaffoldState(
                title = "新增训练动作",
                actionItems = listOf(
                    TopAction.textPageAction(
                        text = "SAVE",
                        type = PageHandleType.SAVE,
                        valid = state.inputValid,
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
                viewModel.reduce(SubmitIntent)
            }
        }
    }
    val scaffoldProperty = LocalScaffoldProperty.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldProperty.padding)
            .padding(horizontal = 12.dp)
            .nestedScroll(scaffoldProperty.scrollConnection),
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
            value = state.name,
            onValueChange = { viewModel.reduce(SetNameIntent(it)) },
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
                selected = state.isTimed,
                onClick = { viewModel.reduce(SetTimedIntent(state.isTimed.not())) },
                label = { Text(text = "计时动作") },
                leadingIcon = {
                    if (state.isTimed) {
                        CheckedIcon()
                    }
                }
            )
            FilterChip(
                selected = state.isWeighted,
                onClick = { viewModel.reduce(SetWeightedIntent(state.isWeighted.not())) },
                label = { Text(text = "计重动作") },
                leadingIcon = {
                    if (state.isWeighted) {
                        CheckedIcon()
                    }
                }
            )
            FilterChip(
                selected = state.isCounted,
                onClick = { viewModel.reduce(SetCountedIntent(state.isCounted.not())) },
                label = { Text(text = "计次动作") },
                leadingIcon = {
                    if (state.isCounted) {
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
    private val reducer: AddTrainActionReducer,
) : ViewModel() {
    private val _state = MutableStateFlow(AddTrainActionState())
    val state = _state.asStateFlow()

    fun initData(partId: Int, actionId: Int = 0) {
        Log.i("AddTrainAction", "Load action with part id $partId, action id $actionId")
        reduce(InitLoadIntent(partId = partId, actionId = actionId))
    }

    fun reduce(intent: IAddTrainActionIntent) {
        viewModelScope.launch {
            val current = _state.value
            val result = reducer.reduce(current, intent)
            _state.emit(result.state)
            result.sideEffect.collect {
                reduce(it)
            }
        }
    }
}

data class AddTrainActionState(
    val part: TrainPart? = null,
    val action: TrainAction? = null,
    val status: ActionStatus = ActionStatus.Idle,
    val isTimed: Boolean = false,
    val isCounted: Boolean = false,
    val isWeighted: Boolean = false,
) {
    val inputValid = name.isNotBlank() && part != null
    val name: String
        get() = action?.actionName ?: ""
}