package site.xiaozk.dailyfitness.page.body

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.repository.IPersonDailyRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import java.time.LocalDate
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
@HiltViewModel
class BodyViewModel @Inject constructor(
    private val bodyRepo: IPersonDailyRepository,
    private val userRepo: IUserRepository,
) : ViewModel() {
    val bodyDetail: Flow<BodyDataWithDate> = flow {
        val user = userRepo.getCurrentUser()
        emitAll(
            bodyRepo.getPersonDailyDataFlow(
                user,
                LocalDate.now().withDayOfMonth(1),
                LocalDate.now().withDayOfMonth(30)
            )
        )
    }

    private val _deleteAction = MutableStateFlow<ActionStatus>(ActionStatus.Idle)
    val deleteAction = _deleteAction.asStateFlow()

    fun deleteBodyDetail(data: BodyDataRecord) {
        viewModelScope.launch {
            _deleteAction.emit(ActionStatus.Loading)
            try {
                bodyRepo.removePersonDailyData(data)
                _deleteAction.emit(ActionStatus.Done)
            } catch (e: Exception) {
                _deleteAction.emit(ActionStatus.Failed(e))
            }
        }
    }
}