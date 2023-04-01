package site.xiaozk.dailyfitness.page.training

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
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
}