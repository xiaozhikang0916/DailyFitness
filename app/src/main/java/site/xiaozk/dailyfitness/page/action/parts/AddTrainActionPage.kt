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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AddFailedSnackbar
import site.xiaozk.dailyfitness.nav.AddSuccessSnackbar
import site.xiaozk.dailyfitness.nav.LocalAppSnackbarHostState
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.widget.DialogPageScaffold
import site.xiaozk.dailyfitness.widget.LargeDropdownMenu
import site.xiaozk.dailyfitness.widget.ScaffoldProperty
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/28
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainActionPage(partId: Int, actionId: Int) {
    val viewModel = hiltViewModel<AddTrainActionViewModel, AddTrainActionViewModel.Factory>(
        creationCallback = { it.create(partId, actionId) }
    )
    val state = viewModel.state.collectAsState().value
    val status = state.status
    val systemBack = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val appSnackbarHostState = LocalAppSnackbarHostState.current
    val title = stringResource(if (state.action?.id != 0) R.string.edit_train_action else R.string.new_train_action)
    val actionLabel = stringResource(R.string.top_action_save)
    LaunchedEffect(key1 = status) {
        if (status == ActionStatus.Done) {
            appSnackbarHostState.showSnackbar(AddSuccessSnackbar)
            systemBack?.onBackPressed()
        } else if (status is ActionStatus.Failed) {
            appSnackbarHostState.showSnackbar(AddFailedSnackbar)
        }
    }
    DialogPageScaffold(
        title = title,
        onBack = { systemBack?.onBackPressed() },
        actions = {
            TextButton(
                onClick = { viewModel.reduce(SubmitIntent) },
                enabled = state.inputValid,
            ) {
                Text(actionLabel)
            }
        }
    ) { scaffoldProperty ->
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
}

@Composable
private fun CheckedIcon() {
    Icon(
        painter = rememberVectorPainter(image = Icons.Default.Done),
        modifier = Modifier.size(18.dp),
        contentDescription = null,
    )
}

@HiltViewModel(assistedFactory = AddTrainActionViewModel.Factory::class)
class AddTrainActionViewModel @AssistedInject constructor(
    private val reducer: AddTrainActionReducer,
    @Assisted("partId") private val partId: Int,
    @Assisted("actionId") private val actionId: Int,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("partId") partId: Int,
            @Assisted("actionId") actionId: Int,
        ): AddTrainActionViewModel
    }

    private val _state = MutableStateFlow(AddTrainActionState())
    val state = _state.asStateFlow()

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