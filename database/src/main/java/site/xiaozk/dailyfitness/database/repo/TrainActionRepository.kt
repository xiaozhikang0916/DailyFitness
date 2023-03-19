package site.xiaozk.dailyfitness.database.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.model.toDbEntity
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
            TrainPartPage(map.entries.map {
                it.toRepoEntity()
            })
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
        trainDao.addTrainPart(part.toDbEntity())
    }

    override suspend fun updateTrainPart(part: TrainPart) {
        trainDao.updateTrainPart(part.toDbEntity())
    }

    override suspend fun removeTrainPart(part: TrainPart) {
        trainDao.deleteTrainPart(part.toDbEntity())
    }

    override fun getAction(actionId: Int): Flow<TrainAction> {
        return trainDao.getTrainAction(actionId).map { it.toRepoAction() }
    }

    override suspend fun addTrainAction(action: TrainAction) {
        trainDao.addTrainAction(
            action.toDbEntity()
        )
    }

    override suspend fun updateTrainAction(action: TrainAction) {
        trainDao.updateTrainAction(action.toDbEntity())
    }

    override suspend fun removeTrainAction(action: TrainAction) {
        trainDao.deleteTrainAction(action.toDbEntity())
    }
}