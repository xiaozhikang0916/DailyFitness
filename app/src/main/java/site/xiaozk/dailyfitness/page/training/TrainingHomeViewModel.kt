package site.xiaozk.dailyfitness.page.training

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.WorkoutDayList
import site.xiaozk.dailyfitness.repository.model.User
import java.time.LocalDate
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/25
 */
@HiltViewModel
class TrainingHomeViewModel @Inject constructor(
    private val homeRepo: IDailyWorkoutRepository,
    private val userRepository: IUserRepository,
) : ViewModel() {
    var user: User? = null
        private set

    val pageData: Flow<WorkoutDayList> = flow {
        val user = user ?: userRepository.getCurrentUser()
        emitAll(getHomePageData(user))
    }

    fun getHomePageData(user: User): Flow<WorkoutDayList> {
        return homeRepo.getWorkoutDayList(user, LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(30))
    }
}