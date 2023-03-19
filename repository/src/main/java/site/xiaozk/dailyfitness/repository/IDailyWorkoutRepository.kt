package site.xiaozk.dailyfitness.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.WorkoutDayList
import site.xiaozk.dailyfitness.repository.model.User
import java.time.LocalDate

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
interface IDailyWorkoutRepository {
    fun getWorkoutDayList(user: User, from: LocalDate, to: LocalDate): Flow<WorkoutDayList>

    fun getWorkoutOfDayFlow(user: User, day: LocalDate): Flow<DailyWorkout?> {
        return getWorkoutDayList(user, day, day).map { it.trainedDate[day] }
    }

    suspend fun addWorkoutAction(user: User, action: DailyWorkoutAction)

    suspend fun deleteWorkoutAction(user: User, action: DailyWorkoutAction)

    fun getAllTrainParts(): Flow<List<TrainPartGroup>>

    suspend fun getLastWorkout(date: LocalDate = LocalDate.now()): DailyWorkoutAction?
}