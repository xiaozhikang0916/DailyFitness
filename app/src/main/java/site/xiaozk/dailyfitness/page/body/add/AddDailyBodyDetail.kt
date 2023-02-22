@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.body.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
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
            TopAppBar(title = { Text(text = "记录身体数据") }, navigationIcon = { BackButton() })
        },
        modifier = Modifier.systemBarsPadding()
    ) {
        AddDailyBodyDetail(pageState.value, padding = it, viewModel::reduce)
    }
}

@Composable
fun AddDailyBodyDetail(state: AddDailyBodyState, padding: PaddingValues = PaddingValues(), onIntent: (IDailyBodyIntent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        InputField.values().forEach {
            OutlinedTextField(
                value = state.bodyField.getField(it),
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth(),
                onValueChange = { input -> onIntent(InputIntent(input, it)) },
                label = {
                    Text(text = it.label)
                },
                trailingIcon = {
                    Text(text = it.trailing)
                },
                supportingText = {
                    if (state.bodyField.checkFieldValid(it).not()) {
                        Text(text = "输入数字有误", color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        Button(onClick = { onIntent(SubmitBodyIntent) }, modifier = Modifier.padding(all = 4.dp)) {
            Text(text = "Submit", modifier = Modifier.fillMaxWidth())
        }
    }
}