@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.page.action.parts

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.widget.BackButton
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainPartPage() {
    val viewModel: AddTrainPartViewModel = hiltViewModel()
    val state = viewModel.status.collectAsState()
    val nav = LocalNavController.current
    LaunchedEffect(key1 = state.value) {
        if (state.value == ActionStatus.Done) {
            nav?.popBackStack()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "新增训练部位") }, navigationIcon = {
                BackButton()
            })
        },
        modifier = Modifier.systemBarsPadding()
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(it)) {
            var name by remember {
                mutableStateOf("")
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth()
            )

            Button(
                onClick = { viewModel.addPart(name) },
                enabled = state.value == ActionStatus.Idle,
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "Submit")
            }
        }
    }
}

@HiltViewModel
class AddTrainPartViewModel @Inject constructor(
    private val repo: ITrainActionRepository,
) : ViewModel() {
    private val _status = MutableStateFlow<ActionStatus>(ActionStatus.Idle)
    val status = _status.asStateFlow()
    fun addPart(name: String) {
        viewModelScope.launch {
            _status.emit(ActionStatus.Loading)
            try {
                repo.addTrainPart(TrainPart(partName = name))
                _status.emit(ActionStatus.Done)
            } catch (e: Exception) {
                Log.e("AddTrainPart", "Add part failed", e)
                _status.emit(ActionStatus.Failed(e))
            }
        }
    }
}