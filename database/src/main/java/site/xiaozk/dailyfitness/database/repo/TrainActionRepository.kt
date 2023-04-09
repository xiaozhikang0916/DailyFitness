package site.xiaozk.dailyfitness.database.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.model.toDbEntity
import site.xiaozk.dailyfitness.database.model.toHomeTrainPartPage
import site.xiaozk.dailyfitness.database.model.toRepoEntity
import site.xiaozk.dailyfitness.database.model.toTrainActionStatics
import site.xiaozk.dailyfitness.database.model.toTrainPartStaticPage
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */
class TrainActionRepository @Inject constructor(
    private val trainDao: TrainDao,
) : ITrainActionRepository {
    override fun getAllTrainPartStatics(): Flow<HomeTrainPartPage> {
        return trainDao.getAllTrainActionWithWorkout().map {
            trainDao.getAllTrainPart().map { part ->
                part to it.filterKeys { it.partId == part.id }
            }.associate { entry ->
                entry.first to entry.second.toMap()
            }.toHomeTrainPartPage()
        }
    }

    override fun getTrainPartStatic(partId: Int): Flow<TrainPartStaticPage?> {
        return trainDao.getTrainActionWithWorkoutOfPart(partId).map {
            val part = trainDao.getTrainPart(partId).firstOrNull()
            part?.let { p ->
                p to it
            }
        }.map {
            it?.toTrainPartStaticPage()
        }
    }

    override fun getTrainActionStatic(actionId: Int): Flow<TrainActionStaticPage?> {
        return trainDao.getTrainActionWithWorkout(actionId).map {
            it.firstNotNullOfOrNull { it.takeIf { it.key.id == actionId } }?.toPair()?.toTrainActionStatics()
        }
    }

    override fun getAllTrainParts(): Flow<List<TrainPartGroup>> {
        return trainDao.getAllTrainPartWithAction().map {
            it.map { group -> group.toRepoEntity() }
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