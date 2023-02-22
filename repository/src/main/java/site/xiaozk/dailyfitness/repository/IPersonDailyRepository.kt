package site.xiaozk.dailyfitness.repository

import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.User
import java.time.LocalDate

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
interface IPersonDailyRepository {
    fun getPersonDailyDataFlow(user: User, from: LocalDate, to: LocalDate): Flow<BodyDataWithDate>
    suspend fun addPersonDailyData(user: User, data: BodyDataRecord)
}