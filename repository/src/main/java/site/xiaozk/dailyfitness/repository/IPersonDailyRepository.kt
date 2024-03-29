package site.xiaozk.dailyfitness.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import site.xiaozk.dailyfitness.repository.model.User

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
interface IPersonDailyRepository {
    fun getPersonDailyDataFlow(user: User, from: LocalDate, to: LocalDate): Flow<BodyDataWithDate>
    suspend fun addPersonDailyData(user: User, data: BodyDataRecord)

    suspend fun updatePersonDailyData(user: User, data: BodyDataRecord)

    suspend fun addOrUpdatePersonData(user: User, data: BodyDataRecord) {
        if (data.id != 0) {
            updatePersonDailyData(user, data)
        } else {
            addPersonDailyData(user, data)
        }
    }
    suspend fun removePersonDailyData(data: BodyDataRecord)
}