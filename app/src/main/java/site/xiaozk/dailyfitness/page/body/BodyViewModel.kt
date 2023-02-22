package site.xiaozk.dailyfitness.page.body

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import site.xiaozk.dailyfitness.repository.IPersonDailyRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
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
        emitAll(bodyRepo.getPersonDailyDataFlow(user, LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(30)))
    }
}