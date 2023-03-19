package site.xiaozk.dailyfitness.page.training

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import java.time.LocalDate
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
@HiltViewModel
class TrainingDayDetailViewModel @Inject constructor(
    private val trainRepo: IDailyWorkoutRepository,
    private val userRepo: IUserRepository,
) : ViewModel() {
    fun getTrainingData(date: LocalDate): Flow<DailyWorkout?> = flow {
        val user = userRepo.getCurrentUser()
        emitAll(
            trainRepo.getWorkoutOfDayFlow(user, date)
        )
    }

    fun addTrainingAction(action: DailyWorkoutAction) {
        viewModelScope.launch {
            val user = userRepo.getCurrentUser()
            trainRepo.addWorkoutAction(user, action)
        }
    }

    fun removeTrainAction(action: DailyWorkoutAction) {
        viewModelScope.launch {
            Log.i("TrainingDayDetail", "deleting action $action")
            val user = userRepo.getCurrentUser()
            try {
                trainRepo.deleteWorkoutAction(user, action)
                Log.i("TrainingDayDetail", "delete action ${action.id} done")
            } catch (e: Exception) {
                Log.e("TrainingDayDetail", "delete action ${action.id} failed", e)
            }
        }
    }
}