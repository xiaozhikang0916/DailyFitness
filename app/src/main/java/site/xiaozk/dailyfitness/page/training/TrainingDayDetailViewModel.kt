package site.xiaozk.dailyfitness.page.training

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkout

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
@HiltViewModel(assistedFactory = TrainingDayDetailViewModel.Factory::class)
class TrainingDayDetailViewModel @AssistedInject constructor(
    private val trainRepo: IDailyWorkoutRepository,
    private val userRepo: IUserRepository,
    @Assisted private val date: LocalDate,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(date: LocalDate): TrainingDayDetailViewModel
    }

    val trainingData: Flow<DailyWorkout?> = flow {
        val user = userRepo.getCurrentUser()
        emitAll(
            trainRepo.getWorkoutOfDayFlow(user, date)
        )
    }
}