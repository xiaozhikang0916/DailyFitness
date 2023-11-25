package site.xiaozk.dailyfitness.page.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
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
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val date : LocalDate
        get() = savedStateHandle.get<String>("date")
            ?.let { TrainingDayGroup.TrainDayNavItem.fromArgument(it) }
            ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    @OptIn(ExperimentalCoroutinesApi::class)
    val trainingData: Flow<DailyWorkout?> = savedStateHandle.getStateFlow("date", "").map {
        TrainingDayGroup.TrainDayNavItem.fromArgument(it)
    }.transformLatest {
        emitAll(getTrainingData(it))
    }
    private fun getTrainingData(date: LocalDate): Flow<DailyWorkout?> = flow {
        val user = userRepo.getCurrentUser()
        emitAll(
            trainRepo.getWorkoutOfDayFlow(user, date)
        )
    }
}