@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.body.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.widget.BackButton

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDailyBodyDetail() {
    val viewModel: AddDailyBodyViewModel = hiltViewModel()
    val pageState = viewModel.stateFlow.collectAsState()
    val nav = LocalNavController.current
    LaunchedEffect(key1 = pageState.value.submitStatus) {
        if (pageState.value.submitStatus == ActionStatus.Done) {
            nav?.popBackStack()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "记录身体数据") },
                navigationIcon = { BackButton(Icons.Default.Close) },
                actions = {
                    TextButton(onClick = { viewModel.reduce(SubmitBodyIntent) }) {
                        Text(text = "SAVE")
                    }
                }
            )
        },
        modifier = Modifier.systemBarsPadding(),
    ) {
        AddDailyBodyDetail(pageState.value, padding = it, viewModel::reduce)
    }
}

@Composable
fun AddDailyBodyDetail(state: AddDailyBodyState, padding: PaddingValues = PaddingValues(), onIntent: (IDailyBodyIntent) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val focus = InputField.values().map { FocusRequester() }
        itemsIndexed(
            InputField.values(),
            span = { _, it ->
                when (it) {
                    InputField.Weight -> GridItemSpan(2)
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
                        Text(text = "输入数字有误")
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = if (index < InputField.values().size - 1) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onNext = { focus.getOrNull(index + 1)?.requestFocus() }),
                isError = state.bodyField.checkFieldValid(it).not()
            )
        }
    }
}