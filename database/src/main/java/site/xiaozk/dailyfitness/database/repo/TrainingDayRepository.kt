package site.xiaozk.dailyfitness.database.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.xiaozk.dailyfitness.database.dao.DailyDao
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.model.toDbEntity
import site.xiaozk.dailyfitness.database.model.toRepoEntity
import site.xiaozk.dailyfitness.database.model.toTrainingDayList
import site.xiaozk.dailyfitness.database.utils.getEndEpochMillis
import site.xiaozk.dailyfitness.database.utils.getStartEpochMillis
import site.xiaozk.dailyfitness.repository.ITrainingDayRepository
import site.xiaozk.dailyfitness.repository.model.DailyTrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainingDayList
import site.xiaozk.dailyfitness.repository.model.User
import java.time.LocalDate
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */
class TrainingDayRepository @Inject constructor(
    private val dailyDao: DailyDao,
    private val trainDao: TrainDao,
) : ITrainingDayRepository {
    override fun getTrainingDayList(
        user: User,
        from: LocalDate,
        to: LocalDate,
    ): Flow<TrainingDayList> {
        return dailyDao.getDailyTrainingActions(
            user.uid,
            from.getStartEpochMillis(),
            to.getEndEpochMillis()
        ).map {
            val actions = it.keys.map { it.id }.toIntArray()
            val parts = dailyDao.getTrainPartOfAction(actions)
            it.entries.groupBy({ entry -> parts[entry.key.id] }) { entry ->
                entry.toPair()
            }.mapNotNull { entry ->
                entry.key?.let { key -> key to entry.value.toMap() }
            }.toMap().toTrainingDayList()
        }
    }

    override suspend fun addTrainAction(user: User, action: DailyTrainAction) {
        dailyDao.addDailyTrainAction(
            action.toDbEntity(user.uid)
        )
    }

    override fun getAllTrainParts(): Flow<List<TrainPartGroup>> {
        return trainDao.getAllTrainPartWithAction().map {
            it.map { group -> group.toRepoEntity() }
        }
    }
}