package site.xiaozk.dailyfitness.database.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.model.toRepoEntity
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainPartPage
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */
class TrainActionRepository @Inject constructor(
    private val trainDao: TrainDao,
) : ITrainActionRepository {
    override fun getAllActions(): Flow<TrainPartPage> {
        return trainDao.getAllTrainPartWithAction().map { map ->
            TrainPartPage(
                map.entries.map {
                    it.toRepoEntity()
                }
            )
        }
    }

    override fun getActionsOfPart(partId: Int): Flow<TrainPartGroup> {
        return trainDao.getAllTrainActionOnPart(partId).mapNotNull { map ->
            map.entries.filter { it.key.id == partId }.map {
                it.toRepoEntity()
            }.firstOrNull()
        }
    }

    override suspend fun addTrainPart(part: TrainPart) {
        trainDao.addTrainPart(site.xiaozk.dailyfitness.database.model.DBTrainPart(partName = part.partName))
    }

    override suspend fun addTrainAction(action: TrainAction) {
        trainDao.addTrainAction(
            site.xiaozk.dailyfitness.database.model.DBTrainAction(
                actionName = action.actionName,
                partId = action.part.id,
                isCountedAction = action.isCountedAction,
                isTimedAction = action.isTimedAction,
                isWeightedAction = action.isWeightedAction
            )
        )
    }
}