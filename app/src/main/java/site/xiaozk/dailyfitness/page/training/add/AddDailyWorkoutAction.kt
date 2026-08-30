@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.training.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.delay
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AddFailedSnackbar
import site.xiaozk.dailyfitness.nav.AddSuccessSnackbar
import site.xiaozk.dailyfitness.nav.LocalAppSnackbarHostState
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit
import site.xiaozk.dailyfitness.widget.DialogPageScaffold
import site.xiaozk.dailyfitness.widget.LargeDropdownMenu
import site.xiaozk.dailyfitness.widget.ScaffoldProperty
import site.xiaozk.dailyfitness.widget.SegmentedControl

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */

@Composable
fun AddDailyWorkoutAction() {
    val viewModel: AddDailyWorkoutViewModel = hiltViewModel()
    val pageState = viewModel.stateFlow.collectAsState()

    val systemBack = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val appSnackbarHostState = LocalAppSnackbarHostState.current
    val title = stringResource(R.string.title_add_workout)
    val actionSave = stringResource(id = R.string.top_action_save)
    LaunchedEffect(pageState.value.submitStatus) {
        if (pageState.value.submitStatus == ActionStatus.Done) {
            appSnackbarHostState.showSnackbar(AddSuccessSnackbar)
            systemBack?.onBackPressed()
        } else if (pageState.value.submitStatus is ActionStatus.Failed) {
            appSnackbarHostState.showSnackbar(AddFailedSnackbar)
        }
    }
    DialogPageScaffold(
        title = title,
        onBack = { systemBack?.onBackPressed() },
        actions = {
            TextButton(
                onClick = { viewModel.reduce(SubmitIntent) },
                enabled = pageState.value.valid,
            ) {
                Text(actionSave)
            }
        }
    ) { scaffoldProperty ->
        AddDailyTrainPage(
            pageState = pageState.value,
            onIntent = { viewModel.reduce(it) },
            scaffoldProperty = scaffoldProperty,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddDailyTrainPage(
    pageState: AddDailyWorkoutPageState,
    onIntent: (IDailyTrainIntent) -> Unit,
    scaffoldProperty: ScaffoldProperty = ScaffoldProperty(),
) {
    val allParts = pageState.allParts
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldProperty.padding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        LargeDropdownMenu(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            label = stringResource(id = R.string.title_train_part),
            expended = pageState.showPartMenuState,
            selectedIndex = pageState.allParts.indexOf(pageState.selectedPart),
            items = allParts,
            onItemSelected = { _, it ->
                onIntent(SelectPartIntent(it))
            },
            itemToString = { it.part.partName },
        )

        val selectedPart = pageState.selectedPart

        if (selectedPart != null) {
            LargeDropdownMenu(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                label = stringResource(id = R.string.title_train_action),
                expended = pageState.showActionMenuState,
                selectedIndex = selectedPart.actions.indexOf(pageState.selectedAction),
                items = selectedPart.actions,
                onItemSelected = { _, it ->
                    onIntent(SelectActionIntent(it))
                },
                itemToString = { it.actionName },
            )
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        val selectedAction = pageState.selectedAction
        if (selectedAction != null) {
            // 输入字段显示顺序：重量 -> 时长 -> 次数 -> 备注
            val focusRequesters = remember {
                Array(WorkoutField.entries.size) { FocusRequester() }
            }
            // 当前动作可见的输入字段（按显示顺序，备注始终可见）
            val visibleFields = buildList {
                if (selectedAction.isWeightedAction) add(WorkoutField.Weight)
                if (selectedAction.isTimedAction) add(WorkoutField.Time)
                if (selectedAction.isCountedAction) add(WorkoutField.Count)
                add(WorkoutField.Note)
            }
            fun requesterOf(field: WorkoutField) = focusRequesters[field.ordinal]
            fun focusNext(field: WorkoutField) {
                visibleFields.getOrNull(visibleFields.indexOf(field) + 1)
                    ?.let { requesterOf(it).requestFocus() }
            }
            val first = requesterOf(visibleFields.first())
            // 每次切换动作时字段集合都会变化（如从“仅次数”切到“重量+次数”），
            // 以 selectedAction 为 key，切换后重新把焦点请求到当前第一个输入框；
            // 新字段刚加入组合、焦点节点可能尚未附加，重试直到请求成功。
            LaunchedEffect(key1 = selectedAction) {
                while (!first.requestFocus()) {
                    delay(16)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (selectedAction.isWeightedAction) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        WorkoutInput(
                            value = pageState.weight,
                            label = stringResource(id = R.string.label_workout_weight),
                            valid = pageState.weightValid,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(requesterOf(WorkoutField.Weight)),
                            onValueChange = {
                                onIntent(
                                    InputWeightIntent(
                                        it,
                                        weightUnit = pageState.weightUnit
                                    )
                                )
                            },
                            onNextFocus = { focusNext(WorkoutField.Weight) }
                        )

                        WeightRadio(
                            pageState = pageState,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            onIntent(InputWeightIntent(weight = pageState.weight, weightUnit = it))
                        }
                    }

                }
                if (selectedAction.isTimedAction) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        WorkoutInput(
                            value = pageState.duration,
                            label = stringResource(id = R.string.label_workout_duration),
                            valid = pageState.timeValid,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(requesterOf(WorkoutField.Time)),
                            onValueChange = {
                                onIntent(
                                    InputDurationIntent(
                                        it,
                                        timeUnit = pageState.timeUnit
                                    )
                                )
                            },
                            onNextFocus = { focusNext(WorkoutField.Time) }
                        )

                        TimeUnitRadio(pageState = pageState, modifier = Modifier.padding(top = 8.dp)) {
                            onIntent(InputDurationIntent(duration = pageState.duration, timeUnit = it))
                        }
                    }
                }

                if (selectedAction.isCountedAction) {
                    WorkoutInput(
                        value = pageState.count,
                        label = stringResource(id = R.string.label_workout_count),
                        valid = pageState.countValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(requesterOf(WorkoutField.Count)),
                        onValueChange = {
                            onIntent(InputCountIntent(it))
                        },
                        onNextFocus = { focusNext(WorkoutField.Count) }
                    )
                }

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(requesterOf(WorkoutField.Note)),
                    value = pageState.note,
                    singleLine = true,
                    label = {
                        Text(text = stringResource(R.string.label_workout_note))
                    },
                    onValueChange = { onIntent(InputNoteIntent(it)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onIntent(SubmitIntent)
                        }
                    )
                )
            }
        }
    }
}

/**
 * 训练录入页的输入字段，声明顺序即输入框显示顺序。
 */
private enum class WorkoutField { Weight, Time, Count, Note }

@Composable
private fun WorkoutInput(
    value: String,
    label: String,
    valid: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit = {},
    onNextFocus: () -> Unit = {},
) {
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    var focused by remember {
        mutableStateOf(false)
    }
    LaunchedEffect(key1 = focused) {
        textFieldValue = if (focused) {
            textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length))
        } else {
            textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
        }
    }
    OutlinedTextField(
        modifier = modifier
            .onFocusChanged {
                focused = it.isFocused
            },
        value = textFieldValue,
        label = {
            Text(text = label)
        },
        singleLine = true,
        supportingText = {
            if (valid.not()) {
                Text(text = stringResource(id = R.string.hint_invalid_input_num))
            }
        },
        isError = valid.not(),
        onValueChange = {
            onValueChange(it.text)
        },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(
            onNext = {
                onNextFocus()
            }
        ),
    )
}

@Composable
private fun WeightRadio(
    pageState: AddDailyWorkoutPageState,
    modifier: Modifier = Modifier,
    onWeightUnitSelect: (WeightUnit) -> Unit,
) {
    val list = WeightUnit.values().toList()
    SegmentedControl(
        items = list,
        modifier = modifier,
        itemWidth = 72.dp,
        itemToString = { it.name },
        defaultSelectedItemIndex = list.indexOf(pageState.weightUnit),
        onItemSelection = onWeightUnitSelect
    )

}

@Composable
private fun TimeUnitRadio(
    pageState: AddDailyWorkoutPageState,
    modifier: Modifier = Modifier,
    onTimeUnitSelect: (TimeUnit) -> Unit,
) {
    val list = TimeUnit.values().toList()
    SegmentedControl(
        items = list,
        modifier = modifier,
        itemWidth = 72.dp,
        itemToString = { it.name },
        defaultSelectedItemIndex = list.indexOf(pageState.timeUnit),
        onItemSelection = onTimeUnitSelect
    )
}