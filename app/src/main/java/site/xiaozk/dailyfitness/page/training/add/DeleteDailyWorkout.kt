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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaInstant
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.DelFailedSnackbar
import site.xiaozk.dailyfitness.nav.DelSuccessSnackbar
import site.xiaozk.dailyfitness.nav.LoadFailedSnackbar
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
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
fun DeleteDailyWorkout() {
    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    val viewModel: DeleteDailyWorkoutViewModel = hiltViewModel()
    val dismiss = remember {
        {
            appScaffoldViewModel.back()
        }
    }
    val state = viewModel.flow.collectAsState(initial = null).value
    LaunchedEffect(key1 = Unit) {
        viewModel.flow.collect {
            if (it.loadStatus is ActionStatus.Failed) {
                appScaffoldViewModel.showSnackbarAndBack(LoadFailedSnackbar)
            }
            if (it.deleteStatus is ActionStatus.Done) {
                appScaffoldViewModel.showSnackbarAndBack(DelSuccessSnackbar)
            }
            if (it.deleteStatus is ActionStatus.Failed) {
                appScaffoldViewModel.showSnackbarAndBack(DelFailedSnackbar)
            }
        }
    }

    val workout = state?.workout
    if (workout != null) {
        AlertDialog(
            onDismissRequest = dismiss,
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
                        .clickable { dismiss() },
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

@HiltViewModel
class DeleteDailyWorkoutViewModel @Inject constructor(
    private val trainRepo: IDailyWorkoutRepository,
    private val userRepo: IUserRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val workoutId: Int
        get() = savedStateHandle["workoutId"] ?: -1
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