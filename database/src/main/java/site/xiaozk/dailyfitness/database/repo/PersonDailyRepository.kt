package site.xiaozk.dailyfitness.database.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.xiaozk.dailyfitness.database.dao.BodyDao
import site.xiaozk.dailyfitness.database.model.toDbEntity
import site.xiaozk.dailyfitness.repository.IPersonDailyRepository
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import site.xiaozk.dailyfitness.repository.model.User
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */
class PersonDailyRepository @Inject constructor(
    private val bodyDao: BodyDao,
) : IPersonDailyRepository {
    override fun getPersonDailyDataFlow(user: User, from: LocalDate, to: LocalDate): Flow<BodyDataWithDate> {
        return bodyDao.getPersonDailyDataFlow(user, from, to).map {
            val map: Map<LocalDate, List<BodyDataRecord>> = it.groupBy({ it.recordTime.atZone(ZoneId.systemDefault()).toLocalDate() }) {
                it.toRepoEntity()
            }
            BodyDataWithDate(
                map
            )
        }
    }

    override suspend fun addPersonDailyData(user: User, data: BodyDataRecord) {
        bodyDao.addDailyPersonData(
            data = data.toDbEntity(user.uid)
        )
    }

    override suspend fun updatePersonDailyData(user: User, data: BodyDataRecord) {
        bodyDao.updateDailyPersonData(
            data = data.toDbEntity(user.uid)
        )
    }

    override suspend fun removePersonDailyData(data: BodyDataRecord) {
        bodyDao.deleteDailyPersonData(data = data.toDbEntity())

    }
}