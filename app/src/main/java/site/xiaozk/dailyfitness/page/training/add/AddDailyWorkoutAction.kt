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
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.hilt.navigation.compose.hiltViewModel
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
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit
import site.xiaozk.dailyfitness.widget.LargeDropdownMenu
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

    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    val title = stringResource(R.string.title_add_workout)
    val actionSave = stringResource(id = R.string.top_action_save)
    LaunchedEffect(key1 = pageState.value.valid) {
        appScaffoldViewModel.scaffoldState.emit(
            FullDialogScaffoldState(
                title = title,
                actionItems = listOf(
                    TopAction.textPageAction(actionSave, PageHandleType.SAVE, pageState.value.valid)
                )
            )
        )
    }
    LaunchedEffect(pageState.value.submitStatus) {
        if (pageState.value.submitStatus == ActionStatus.Done) {
            appScaffoldViewModel.showSnackbarAndBack(AddSuccessSnackbar)
        } else if (pageState.value.submitStatus is ActionStatus.Failed) {
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
    AddDailyTrainPage(pageState = pageState.value, onIntent = { viewModel.reduce(it) })
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddDailyTrainPage(
    pageState: AddDailyWorkoutPageState,
    onIntent: (IDailyTrainIntent) -> Unit,
) {
    val scaffoldProperty = LocalScaffoldProperty.current
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

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        val selectedAction = pageState.selectedAction
        if (selectedAction != null) {
            val (weightFocus, timeFocus, countFocus, noteFocus) = remember {
                FocusRequester.createRefs()
            }
            val first = if (selectedAction.isWeightedAction) {
                weightFocus
            } else if (selectedAction.isTimedAction) {
                timeFocus
            } else if (selectedAction.isCountedAction) {
                countFocus
            } else {
                noteFocus
            }
            LaunchedEffect(key1 = Unit) {
                first.requestFocus()
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
                                .focusRequester(weightFocus),
                            onValueChange = {
                                onIntent(
                                    InputWeightIntent(
                                        it,
                                        weightUnit = pageState.weightUnit
                                    )
                                )
                            },
                            onNextFocus = {
                                if (selectedAction.isTimedAction) {
                                    timeFocus.requestFocus()
                                } else if (selectedAction.isCountedAction) {
                                    countFocus.requestFocus()
                                } else {
                                    noteFocus.requestFocus()
                                }
                            }
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
                                .focusRequester(timeFocus),
                            onValueChange = {
                                onIntent(
                                    InputDurationIntent(
                                        it,
                                        timeUnit = pageState.timeUnit
                                    )
                                )
                            },
                            onNextFocus = {
                                if (selectedAction.isCountedAction) {
                                    countFocus.requestFocus()
                                } else {
                                    noteFocus.requestFocus()
                                }
                            }
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
                            .focusRequester(countFocus),
                        onValueChange = {
                            onIntent(InputCountIntent(it))
                        },
                        onNextFocus = {
                            noteFocus.requestFocus()
                        }
                    )
                }

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(noteFocus),
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