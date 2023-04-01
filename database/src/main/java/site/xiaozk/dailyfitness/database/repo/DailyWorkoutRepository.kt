package site.xiaozk.dailyfitness.database.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.xiaozk.dailyfitness.database.dao.DailyDao
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.model.toDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.toDbEntity
import site.xiaozk.dailyfitness.database.model.toRepoEntity
import site.xiaozk.dailyfitness.database.model.toTrainingDayList
import site.xiaozk.dailyfitness.database.utils.getEndEpochMillis
import site.xiaozk.dailyfitness.database.utils.getStartEpochMillis
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.User
import site.xiaozk.dailyfitness.repository.model.WorkoutDayList
import java.time.LocalDate
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */
class DailyWorkoutRepository @Inject constructor(
    private val dailyDao: DailyDao,
    private val trainDao: TrainDao,
) : IDailyWorkoutRepository {
    private var lastWorkout: DailyWorkoutAction? = null

    override fun getWorkoutDayList(
        user: User,
        from: LocalDate,
        to: LocalDate,
    ): Flow<WorkoutDayList> {
        return dailyDao.getDailyWorkoutActions(
            user.uid,
            from.getStartEpochMillis(),
            to.getEndEpochMillis()
        ).map {
            val actions = it.keys.map { it.id }.toIntArray()
            val parts = trainDao.getTrainPartOfAction(actions)
            it.entries.groupBy({ entry -> parts[entry.key.id] }) { entry ->
                entry.toPair()
            }.mapNotNull { entry ->
                entry.key?.let { key -> key to entry.value.toMap() }
            }.toMap().toTrainingDayList()
        }
    }

    override suspend fun getWorkout(user: User, workoutId: Int): DailyWorkoutAction {
        return dailyDao.getDailyWorkout(user.uid, workoutId).filter { it.value.actionId == workoutId }.map { it.toPair().toDailyWorkoutAction() }.first()
    }

    override suspend fun addWorkoutAction(user: User, action: DailyWorkoutAction) {
        val workout = action.toDbEntity(user.uid)
        dailyDao.addDailyWorkoutAction(workout)
        lastWorkout = action
    }

    override suspend fun deleteWorkoutAction(user: User, action: DailyWorkoutAction) {
        dailyDao.deleteDailyWorkoutAction(action.toDbEntity(user.uid))
    }

    override fun getAllTrainParts(): Flow<List<TrainPartGroup>> {
        return trainDao.getAllTrainPartWithAction().map {
            it.map { group -> group.toRepoEntity() }
        }
    }

    override suspend fun getLastWorkout(date: LocalDate): DailyWorkoutAction? {
        return lastWorkout?.takeIf { LocalDate.from(it.instant) == date }
    }
}