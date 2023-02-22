package site.xiaozk.dailyfitness.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.xiaozk.dailyfitness.repository.model.DailyTrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainingDayData
import site.xiaozk.dailyfitness.repository.model.TrainingDayList
import site.xiaozk.dailyfitness.repository.model.User
import java.time.LocalDate

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
interface ITrainingDayRepository {
    fun getTrainingDayList(user: User, from: LocalDate, to: LocalDate): Flow<TrainingDayList>

    fun getTrainingOfDayFlow(user: User, day: LocalDate): Flow<TrainingDayData?> {
        return getTrainingDayList(user, day, day).map { it.trainedDate[day] }
    }

    suspend fun addTrainAction(user: User, action: DailyTrainAction)

    fun getAllTrainParts(): Flow<List<TrainPartGroup>>
}