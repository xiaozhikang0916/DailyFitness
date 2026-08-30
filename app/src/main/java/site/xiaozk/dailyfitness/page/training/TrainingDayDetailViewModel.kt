package site.xiaozk.dailyfitness.page.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.session.WorkoutSessionController
import site.xiaozk.dailyfitness.session.WorkoutSessionState

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
@HiltViewModel(assistedFactory = TrainingDayDetailViewModel.Factory::class)
class TrainingDayDetailViewModel @AssistedInject constructor(
    private val trainRepo: IDailyWorkoutRepository,
    private val userRepo: IUserRepository,
    private val sessionController: WorkoutSessionController,
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

    /** Session state (active / current action / set count) for the start/finish entry. */
    val sessionState: StateFlow<WorkoutSessionState> = sessionController.state

    /** Finishes the session; the service tears itself down on the inactive state. */
    fun finishSession() {
        viewModelScope.launch { sessionController.finish() }
    }
}