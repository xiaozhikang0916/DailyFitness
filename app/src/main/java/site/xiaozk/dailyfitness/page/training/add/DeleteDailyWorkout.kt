package site.xiaozk.dailyfitness.page.training.add

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.toJavaInstant
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.DelFailedSnackbar
import site.xiaozk.dailyfitness.nav.DelSuccessSnackbar
import site.xiaozk.dailyfitness.nav.LoadFailedSnackbar
import site.xiaozk.dailyfitness.nav.LocalAppSnackbarHostState
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.utils.getLocalDateTimeFormatter
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @create: 2023/4/1
 */

@Composable
fun DeleteDailyWorkout(
    workoutId: Int,
    onDismiss: () -> Unit,
) {
    val appSnackbarHostState = LocalAppSnackbarHostState.current
    // 本地状态对话框：每次打开重新组合 → 新 key → 独立 VM 实例
    val vmKey = remember { "delete-workout-$workoutId-${UUID.randomUUID()}" }
    val viewModel = hiltViewModel<DeleteDailyWorkoutViewModel, DeleteDailyWorkoutViewModel.Factory>(
        key = vmKey,
        creationCallback = { it.create(workoutId) }
    )
    val state = viewModel.flow.collectAsState(initial = null).value
    LaunchedEffect(key1 = Unit) {
        viewModel.flow.collect {
            if (it.loadStatus is ActionStatus.Failed) {
                appSnackbarHostState.showSnackbar(LoadFailedSnackbar)
                onDismiss()
            }
            if (it.deleteStatus is ActionStatus.Done) {
                appSnackbarHostState.showSnackbar(DelSuccessSnackbar)
                onDismiss()
            }
            if (it.deleteStatus is ActionStatus.Failed) {
                appSnackbarHostState.showSnackbar(DelFailedSnackbar)
                onDismiss()
            }
        }
    }

    val workout = state?.workout
    if (workout != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Text(
                    text = stringResource(id = R.string.dialog_action_delete),
                    modifier = Modifier
                        .clickable {
                            viewModel.removeWorkout(workout)
                        },
                    textAlign = TextAlign.Center
                )
            },
            dismissButton = {
                Text(
                    text = stringResource(id = R.string.dialog_action_cancel),
                    modifier = Modifier
                        .clickable { onDismiss() },
                    textAlign = TextAlign.Center
                )
            },
            title = {
                Text(text = stringResource(R.string.title_dialog_delete_workout))
            },
            text = {
                val dateTimeFormat =
                    getLocalDateTimeFormatter(Locale.getDefault()).withZone(ZoneId.systemDefault())
                Text(
                    text = stringResource(R.string.desc_dialog_delete_workout, dateTimeFormat.format(workout.instant.toJavaInstant()), workout.action.actionName, workout.displayText.joinToString(" "))
                )
            }
        )
    }
}

@HiltViewModel(assistedFactory = DeleteDailyWorkoutViewModel.Factory::class)
class DeleteDailyWorkoutViewModel @AssistedInject constructor(
    private val trainRepo: IDailyWorkoutRepository,
    private val userRepo: IUserRepository,
    @Assisted private val workoutId: Int,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(workoutId: Int): DeleteDailyWorkoutViewModel
    }

    private val _flow = MutableStateFlow(DeleteDailyWorkoutState())
    val flow = _flow.asStateFlow()

    init {
        viewModelScope.launch {
            Log.i("DeleteDailyWorkout", "Loading workout id $workoutId")
            _flow.emit(DeleteDailyWorkoutState(loadStatus = ActionStatus.Loading))
            try {
                val workout = trainRepo.getWorkout(userRepo.getCurrentUser(), workoutId)
                Log.i("DeleteDailyWorkout", "workout id $workoutId loaded")
                _flow.emit(_flow.value.copy(workout = workout, loadStatus = ActionStatus.Done))
            } catch (e: Exception) {
                Log.e("DeleteDailyWorkout", "workout id $workoutId load failed", e)
                _flow.emit(DeleteDailyWorkoutState(loadStatus = ActionStatus.Failed(e)))
            }
        }
    }

    fun removeWorkout(action: DailyWorkoutAction) {
        viewModelScope.launch {
            Log.i("DeleteDailyWorkout", "deleting action $action")
            val user = userRepo.getCurrentUser()
            _flow.emit(flow.value.copy(deleteStatus = ActionStatus.Loading))
            try {
                trainRepo.deleteWorkoutAction(user, action)
                _flow.emit(flow.value.copy(deleteStatus = ActionStatus.Done))
                Log.i("DeleteDailyWorkout", "delete action ${action.id} done")
            } catch (e: Exception) {
                _flow.emit(flow.value.copy(deleteStatus = ActionStatus.Failed(e)))
                Log.e("DeleteDailyWorkout", "delete action ${action.id} failed", e)
            }
        }
    }
}

data class DeleteDailyWorkoutState(
    val workout: DailyWorkoutAction? = null,
    val loadStatus: ActionStatus = ActionStatus.Idle,
    val deleteStatus: ActionStatus = ActionStatus.Idle,
)