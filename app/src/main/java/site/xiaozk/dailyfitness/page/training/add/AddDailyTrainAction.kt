@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.training.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit
import site.xiaozk.dailyfitness.widget.BackButton

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDailyTrainAction() {
    val viewModel: AddDailyTrainViewModel = hiltViewModel()
    val pageState = viewModel.stateFlow.collectAsState()

    val nav = LocalNavController.current
    LaunchedEffect(pageState.value.submitStatus) {
        if (pageState.value.submitStatus == ActionStatus.Done) {
            nav?.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "训练日志") },
                navigationIcon = {
                    BackButton()
                },
            )
        },
    ) {
        AddDailyTrainPage(pageState = pageState.value, paddingValues = it, onIntent = { viewModel.reduce(it) })
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddDailyTrainPage(pageState: AddDailyTrainPageState, paddingValues: PaddingValues = PaddingValues(), onIntent: (IDailyTrainIntent) -> Unit) {
    val allParts = pageState.allParts
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues), verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(all = 4.dp)
        ) {
            Box {
                Box(
                    modifier = Modifier.height(IntrinsicSize.Min)
                ) {
                    OutlinedTextField(
                        value = pageState.selectedPart?.part?.partName ?: "",
                        label = {
                            Text(text = "训练部位")
                        },
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onIntent(PartMenuIntent(true)) },
                        color = Color.Transparent,
                    ) {}
                }
            }

            DropdownMenu(
                modifier = Modifier.fillMaxWidth(),
                expanded = pageState.showPartMenuState,
                onDismissRequest = { onIntent(PartMenuIntent(false)) }) {
                allParts.forEach {
                    DropdownMenuItem(
                        modifier = Modifier.fillMaxWidth(),
                        text = { Text(text = it.part.partName) },
                        onClick = { onIntent(SelectPartIntent(it)) })
                }
            }
        }

        val selectedPart = pageState.selectedPart
        val actionEnabled = (selectedPart != null)
        Box(
            modifier = Modifier
                .padding(all = 4.dp)
        ) {

            Box(
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                OutlinedTextField(
                    value = pageState.selectedAction?.actionName ?: "",
                    label = {
                        Text(text = "训练动作")
                    },
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth(),
                    enabled = actionEnabled,
                )

                Box(modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = actionEnabled) { onIntent(ActionMenuIntent(true)) })
            }

            if (selectedPart != null) {
                DropdownMenu(
                    modifier = Modifier.fillMaxWidth(),
                    expanded = pageState.showActionMenuState,
                    onDismissRequest = { onIntent(ActionMenuIntent(false)) }) {

                    selectedPart.actions.forEach {
                        DropdownMenuItem(
                            modifier = Modifier.fillMaxWidth(),
                            text = { Text(text = it.actionName) },
                            onClick = { onIntent(SelectActionIntent(it)) })
                    }
                }
            }
        }
        val (weightFocus, timeFocus, countFocus, noteFocus, buttonFocus) = remember {
            FocusRequester.createRefs()
        }
        val selectedAction = pageState.selectedAction
        if (selectedAction != null) {
            if (selectedAction.isWeightedAction) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .fillMaxWidth()
                        .focusRequester(weightFocus),
                    value = pageState.weight,
                    label = {
                        Text(text = "重量")
                    },
                    singleLine = true,
                    onValueChange = {
                        onIntent(
                            InputWeightIntent(
                                it,
                                weightUnit = pageState.weightUnit
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (selectedAction.isTimedAction) {
                                timeFocus.requestFocus()
                            } else if (selectedAction.isCountedAction) {
                                countFocus.requestFocus()
                            } else {
                                buttonFocus.requestFocus()
                            }
                        }
                    )
                )

                WeightRadio(pageState = pageState) {
                    onIntent(InputWeightIntent(weight = pageState.weight, weightUnit = it))
                }
            }
            if (selectedAction.isTimedAction) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .fillMaxWidth()
                        .focusRequester(timeFocus),
                    value = pageState.duration,
                    label = {
                        Text(text = "时长")
                    },
                    singleLine = true,
                    onValueChange = {
                        onIntent(
                            InputDurationIntent(
                                it,
                                timeUnit = pageState.timeUnit
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (selectedAction.isCountedAction) {
                                countFocus.requestFocus()
                            } else {
                                buttonFocus.requestFocus()
                            }
                        }
                    )
                )

                TimeUnitRadio(pageState = pageState) {
                    onIntent(InputDurationIntent(duration = pageState.duration, timeUnit = it))
                }
            }

            if (selectedAction.isCountedAction) {
                OutlinedTextField(
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .fillMaxWidth()
                        .focusRequester(countFocus),
                    value = pageState.count,
                    singleLine = true,
                    label = {
                        Text(text = "次数")
                    },
                    onValueChange = { onIntent(InputCountIntent(it)) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            noteFocus.requestFocus()
                        }
                    )
                )
            }

            OutlinedTextField(
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth()
                    .focusRequester(noteFocus),
                value = pageState.note,
                singleLine = true,
                label = {
                    Text(text = "备注")
                },
                onValueChange = { onIntent(InputNoteIntent(it)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onNext = {
                        buttonFocus.requestFocus()
                    }
                )
            )

            Button(
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth()
                    .focusRequester(buttonFocus),
                onClick = { onIntent(SubmitIntent) },
                enabled = pageState.valid
            ) {
                Text(text = "Submit")
            }
        }
    }
}

@Composable
private fun WeightRadio(
    pageState: AddDailyTrainPageState,
    onWeightUnitSelect: (WeightUnit) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.SpaceEvenly) {}
    WeightUnit.values().forEach {
        Row {
            RadioButton(selected = pageState.weightUnit == it, onClick = { onWeightUnitSelect(it) })
            Text(text = it.name)
        }
    }
}

@Composable
private fun TimeUnitRadio(pageState: AddDailyTrainPageState, onTimeUnitSelect: (TimeUnit) -> Unit) {
    Row(horizontalArrangement = Arrangement.SpaceEvenly) {}
    TimeUnit.values().forEach {
        Row {
            RadioButton(selected = pageState.timeUnit == it, onClick = { onTimeUnitSelect(it) })
            Text(text = it.name)
        }
    }
}