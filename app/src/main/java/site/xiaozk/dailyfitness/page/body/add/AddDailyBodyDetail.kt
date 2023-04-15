@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.body.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import site.xiaozk.dailyfitness.nav.RouteAction
import site.xiaozk.dailyfitness.nav.TopAction
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
import site.xiaozk.dailyfitness.repository.model.BodyField
import site.xiaozk.dailyfitness.utils.label
import site.xiaozk.dailyfitness.utils.trailing

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */

@Composable
fun AddDailyBodyDetail() {
    val viewModel: AddDailyBodyViewModel = hiltViewModel()
    val pageState = viewModel.stateFlow.collectAsState()

    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    val title = stringResource(id = R.string.title_add_body_data)
    val actionSave = stringResource(id = R.string.top_action_save)
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            FullDialogScaffoldState(
                title = title,
                actionItems = listOf(
                    TopAction.textPageAction(
                        text = actionSave,
                        type = PageHandleType.SAVE,
                    )
                )
            )
        )
    }
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.topAction.collect { intent ->
            when (intent.actionType) {
                is PageHandleAction -> {
                    when (intent.actionType.type) {
                        PageHandleType.SAVE -> viewModel.reduce(SubmitBodyIntent)
                    }
                }

                is RouteAction -> {}
            }
        }
    }
    LaunchedEffect(key1 = pageState.value.submitStatus) {
        if (pageState.value.submitStatus == ActionStatus.Done) {
            appScaffoldViewModel.showSnackbarAndBack(AddSuccessSnackbar)
        } else if (pageState.value.submitStatus is ActionStatus.Failed) {
            appScaffoldViewModel.showSnackbar(AddFailedSnackbar)
        }
    }
    AddDailyBodyDetail(pageState.value, viewModel::reduce)
}

@Composable
fun AddDailyBodyDetail(state: AddDailyBodyState, onIntent: (IDailyBodyIntent) -> Unit) {
    val scaffoldProperty = LocalScaffoldProperty.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .nestedScroll(scaffoldProperty.scrollConnection),
        contentPadding = scaffoldProperty.padding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val focus = BodyField.values().map { FocusRequester() }
        itemsIndexed(
            BodyField.values(),
            span = { _, it ->
                when (it) {
                    BodyField.Weight -> GridItemSpan(2)
                    else -> GridItemSpan(1)
                }
            }
        ) { index, it ->
            OutlinedTextField(
                value = state.bodyField.getField(it),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus[index]),
                onValueChange = { input -> onIntent(InputIntent(input, it)) },
                label = {
                    Text(text = it.label)
                },
                trailingIcon = {
                    Text(text = it.trailing)
                },
                supportingText = {
                    if (state.bodyField.checkFieldValid(it).not()) {
                        Text(text = stringResource(R.string.hint_invalid_input_num))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = if (index < BodyField.values().size - 1) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onNext = { focus.getOrNull(index + 1)?.requestFocus() }),
                isError = state.bodyField.checkFieldValid(it).not()
            )
        }
    }
}