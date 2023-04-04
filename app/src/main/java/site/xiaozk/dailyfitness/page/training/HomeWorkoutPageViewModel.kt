package site.xiaozk.dailyfitness.page.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.HomeWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.User
import java.time.DayOfWeek
import java.time.YearMonth
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/25
 */
@HiltViewModel
class HomeWorkoutPageViewModel @Inject constructor(
    private val homeRepo: IDailyWorkoutRepository,
    private val userRepository: IUserRepository,
) : ViewModel() {
    var user: User? = null
        private set

    val pageData: StateFlow<HomeWorkoutPageState> = flow {
        val user = user ?: userRepository.getCurrentUser()
        emitAll(getHomePageData(user))
    }.stateIn(viewModelScope, SharingStarted.Lazily, HomeWorkoutPageState())

    fun getHomePageData(user: User): Flow<HomeWorkoutPageState> = flow {
        emit(HomeWorkoutPageState(loadStatus = ActionStatus.Loading))
        emitAll(
            homeRepo.getHomeWorkoutStatics(
                user = user,
                month = YearMonth.now(),
                firstDayOfWeek = DayOfWeek.SUNDAY,
            ).catch {
                emit(HomeWorkoutPageState(loadStatus = ActionStatus.Failed(it)))
            }.map {
                HomeWorkoutPageState(
                    homePageState = it,
                    loadStatus = ActionStatus.Done
                )
            }
        )
    }
}

data class HomeWorkoutPageState(
    val homePageState: HomeWorkoutStatic = HomeWorkoutStatic(YearMonth.now()),
    val loadStatus: ActionStatus = ActionStatus.Idle,
)