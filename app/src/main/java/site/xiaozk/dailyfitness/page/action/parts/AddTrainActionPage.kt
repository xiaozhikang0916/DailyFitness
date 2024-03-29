@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.action.parts

import android.util.Log
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AddFailedSnackbar
import site.xiaozk.dailyfitness.nav.AddSuccessSnackbar
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.FullDialogScaffoldState
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.PageHandleAction
import site.xiaozk.dailyfitness.nav.PageHandleType
import site.xiaozk.dailyfitness.nav.TopAction
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.widget.LargeDropdownMenu
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/28
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainActionPage() {
    val viewModel: AddTrainActionViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState().value
    val status = state.status
    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    val title = stringResource(if (state.action?.id != 0) R.string.edit_train_action else R.string.new_train_action)
    val actionLabel = stringResource(R.string.top_action_save)
    LaunchedEffect(key1 = state.inputValid) {
        appScaffoldViewModel.scaffoldState.emit(
            FullDialogScaffoldState(
                title = title,
                actionItems = listOf(
                    TopAction.textPageAction(
                        text = actionLabel,
                        type = PageHandleType.SAVE,
                        valid = state.inputValid,
                    )
                )
            )
        )
    }
    LaunchedEffect(key1 = status) {
        if (status == ActionStatus.Done) {
            appScaffoldViewModel.showSnackbarAndBack(AddSuccessSnackbar)
        } else if (status is ActionStatus.Failed) {
            appScaffoldViewModel.showSnackbar(AddFailedSnackbar)
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

        LargeDropdownMenu(
            label = stringResource(R.string.title_train_part),
            modifier = Modifier
                .fillMaxWidth(),
            items = state.allPart,
            onItemSelected = { _, it ->
                viewModel.reduce(SetTrainPartIntent(it.part))
            },
            expended = false,
            selectedIndex = state.selectedPartIndex,
            itemToString = {
                it.part.partName
            },
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.reduce(SetNameIntent(it)) },
            modifier = Modifier
                .fillMaxWidth(),
            label = {
                Text(text = stringResource(R.string.label_train_action_name))
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FilterChip(
                    selected = state.isWeighted,
                    onClick = { viewModel.reduce(SetWeightedIntent(state.isWeighted.not())) },
                    label = { Text(text = stringResource(R.string.add_action_type_weighted)) },
                    leadingIcon = {
                        if (state.isWeighted) {
                            CheckedIcon()
                        }
                    }
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FilterChip(
                    selected = state.isCounted,
                    onClick = { viewModel.reduce(SetCountedIntent(state.isCounted.not())) },
                    label = { Text(text = stringResource(R.string.add_action_type_counted)) },
                    leadingIcon = {
                        if (state.isCounted) {
                            CheckedIcon()
                        }
                    }
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FilterChip(
                    selected = state.isTimed,
                    onClick = { viewModel.reduce(SetTimedIntent(state.isTimed.not())) },
                    label = { Text(text = stringResource(R.string.add_action_type_timed)) },
                    leadingIcon = {
                        if (state.isTimed) {
                            CheckedIcon()
                        }
                    }
                )
            }
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
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(AddTrainActionState())
    val state = _state.asStateFlow()

    val partId: Int
        get() = savedStateHandle["partId"] ?: -1

    val actionId: Int
        get() = savedStateHandle["actionId"] ?: -1

    init {
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
    val allPart: List<TrainPartGroup> = emptyList(),
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

    val selectedPartIndex: Int = allPart.indexOfFirst { it.part == this.part }
}