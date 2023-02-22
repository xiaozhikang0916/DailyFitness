package site.xiaozk.dailyfitness.repository

import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainPartPage

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
interface ITrainActionRepository {
    fun getAllActions(): Flow<TrainPartPage>

    fun getActionsOfPart(partId: Int): Flow<TrainPartGroup>

    suspend fun addTrainPart(part: TrainPart)

    suspend fun addTrainAction(action: TrainAction)
}