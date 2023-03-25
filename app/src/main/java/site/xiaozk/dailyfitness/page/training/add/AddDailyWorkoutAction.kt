@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.training.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit
import site.xiaozk.dailyfitness.widget.BackButton
import site.xiaozk.dailyfitness.widget.LargeDropdownMenu
import site.xiaozk.dailyfitness.widget.SegmentedControl

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDailyWorkoutAction() {
    val viewModel: AddDailyWorkoutViewModel = hiltViewModel()
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
                colors = TopAppBarDefaults.smallTopAppBarColors(),
                navigationIcon = {
                    BackButton(icon = Icons.Default.Close)
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.reduce(SubmitIntent) },
                        enabled = pageState.value.valid
                    ) {
                        Text(text = "SAVE")
                    }
                }
            )
        },
    ) {
        AddDailyTrainPage(pageState = pageState.value, paddingValues = it, onIntent = { viewModel.reduce(it) })
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddDailyTrainPage(
    pageState: AddDailyWorkoutPageState,
    paddingValues: PaddingValues = PaddingValues(),
    onIntent: (IDailyTrainIntent) -> Unit,
) {
    val allParts = pageState.allParts
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        LargeDropdownMenu(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            label = "训练部位",
            expended = pageState.showPartMenuState,
            items = allParts,
            onItemSelected = { it ->
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
                label = "训练动作",
                expended = pageState.showActionMenuState,
                items = selectedPart.actions,
                onItemSelected = { it ->
                    onIntent(SelectActionIntent(it))
                },
                itemToString = { it.actionName },
                onDismiss = {
//                    onIntent(ActionMenuIntent(false))
                },
            )
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        val selectedAction = pageState.selectedAction
        if (selectedAction != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val (weightFocus, timeFocus, countFocus, noteFocus) = remember {
                    FocusRequester.createRefs()
                }
                if (selectedAction.isWeightedAction) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(weightFocus),
                            value = pageState.weight,
                            label = {
                                Text(text = "重量")
                            },
                            singleLine = true,
                            supportingText = {
                                if (pageState.weightValid.not()) {
                                    Text(text = "输入数字有误")
                                }
                            },
                            isError = pageState.weightValid.not(),
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
                                    }
                                }
                            ),
                        )

                        WeightRadio(pageState = pageState, modifier = Modifier.padding(top = 8.dp)) {
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
                        OutlinedTextField(
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(timeFocus),
                            value = pageState.duration,
                            label = {
                                Text(text = "时长")
                            },
                            singleLine = true,
                            supportingText = {
                                if (pageState.timeValid.not()) {
                                    Text(text = "输入数字有误")
                                }
                            },
                            isError = pageState.timeValid.not(),
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
                                    }
                                }
                            ),
                        )

                        TimeUnitRadio(pageState = pageState, modifier = Modifier.padding(top = 8.dp)) {
                            onIntent(InputDurationIntent(duration = pageState.duration, timeUnit = it))
                        }
                    }
                }

                if (selectedAction.isCountedAction) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(countFocus),
                        value = pageState.count,
                        singleLine = true,
                        label = {
                            Text(text = "次数")
                        },
                        onValueChange = { onIntent(InputCountIntent(it)) },
                        supportingText = {
                            if (pageState.countValid.not()) {
                                Text(text = "输入数字有误")
                            }
                        },
                        isError = pageState.countValid.not(),
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
                        .fillMaxWidth()
                        .focusRequester(noteFocus),
                    value = pageState.note,
                    singleLine = true,
                    label = {
                        Text(text = "备注")
                    },
                    onValueChange = { onIntent(InputNoteIntent(it)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                )
            }
        }
    }
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