package site.xiaozk.dailyfitness.repository

import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
interface ITrainActionRepository {
    fun getAllTrainPartStatics(): Flow<HomeTrainPartPage>

    fun getTrainPartStatic(partId: Int): Flow<TrainPartStaticPage>

    fun getTrainActionStatic(actionId: Int): Flow<TrainActionStaticPage>

    fun getAllTrainParts(): Flow<List<TrainPartGroup>>

    fun getActionsOfPart(partId: Int): Flow<TrainPartGroup>

    suspend fun addTrainPart(part: TrainPart)

    suspend fun updateTrainPart(part: TrainPart)

    suspend fun removeTrainPart(part: TrainPart)

    suspend fun addOrUpdateTrainPart(part: TrainPart) {
        if (part.id != 0) {
            updateTrainPart(part)
        } else {
            addTrainPart(part)
        }
    }

    fun getAction(actionId: Int): Flow<TrainAction>

    suspend fun addTrainAction(action: TrainAction)

    suspend fun updateTrainAction(action: TrainAction)

    suspend fun removeTrainAction(action: TrainAction)

    suspend fun addOrUpdateTrainAction(action: TrainAction) {
        if (action.id != 0) {
            updateTrainAction(action)
        } else {
            addTrainAction(action)
        }
    }
}