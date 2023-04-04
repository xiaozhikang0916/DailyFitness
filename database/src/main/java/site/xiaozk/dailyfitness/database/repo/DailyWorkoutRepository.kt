package site.xiaozk.dailyfitness.database.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.xiaozk.dailyfitness.database.dao.BodyDao
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.dao.WorkoutDao
import site.xiaozk.dailyfitness.database.model.toDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.toDbEntity
import site.xiaozk.dailyfitness.database.model.toRepoEntity
import site.xiaozk.dailyfitness.database.model.toWorkoutDailyMap
import site.xiaozk.dailyfitness.database.model.toWorkoutSummary
import site.xiaozk.dailyfitness.database.utils.getEndEpochMillis
import site.xiaozk.dailyfitness.database.utils.getStartEpochMillis
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.model.BodyStatic
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutMap
import site.xiaozk.dailyfitness.repository.model.HomeWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.User
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */
class DailyWorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val bodyDao: BodyDao,
    private val trainDao: TrainDao,
) : IDailyWorkoutRepository {
    private var lastWorkout: DailyWorkoutAction? = null

    override fun getMonthWorkoutStatic(user: User, month: YearMonth, firstDayOfWeek: DayOfWeek): Flow<MonthWorkoutStatic> {
        return workoutDao.getDailyWorkoutActions(
            user.uid,
            month.atDay(1).getStartEpochMillis(),
            month.atEndOfMonth().getEndEpochMillis(),
        ).map {
            val actions = it.keys.map { it.id }.toIntArray()
            val parts = trainDao.getTrainPartOfAction(actions)
            it.entries.groupBy({ entry -> parts[entry.key.id] }) { entry ->
                entry.toPair()
            }.mapNotNull { entry ->
                entry.key?.let { key -> key to entry.value.toMap() }
            }.toMap().toWorkoutSummary()
        }.map { workout ->
            MonthWorkoutStatic(month, workout)
        }
    }

    override fun getHomeWorkoutStatics(user: User, month: YearMonth, firstDayOfWeek: DayOfWeek): Flow<HomeWorkoutStatic> {
        return getMonthWorkoutStatic(user, month, firstDayOfWeek).map { it ->
            val weight = bodyDao.getLastBodyDataWithWeight(userId = user.uid)
            val bustSize = bodyDao.getLastBodyDataWithBustSize(userId = user.uid)
            val waistSize = bodyDao.getLastBodyDataWithWaistSize(userId = user.uid)
            val hipSize = bodyDao.getLastBodyDataWithHipSize(userId = user.uid)
            val bodyFat = bodyDao.getLastBodyDataWithBodyFat(userId = user.uid)
            HomeWorkoutStatic(
                monthStatic = it,
                weight = weight?.let { BodyStatic(it.recordTime, it.weight) },
                bustSize = bustSize?.let { BodyStatic(it.recordTime, it.bustSize) },
                waistSize = waistSize?.let { BodyStatic(it.recordTime, it.waistSize) },
                hipSize = hipSize?.let { BodyStatic(it.recordTime, it.hipSize) },
                bodyFat = bodyFat?.let { BodyStatic(it.recordTime, it.bodyFat) },
            )
        }

    }

    override fun getWorkoutDayList(
        user: User,
        from: LocalDate,
        to: LocalDate,
    ): Flow<DailyWorkoutMap> {
        return workoutDao.getDailyWorkoutActions(
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
            }.toMap().toWorkoutDailyMap()
        }
    }

    override suspend fun getWorkout(user: User, workoutId: Int): DailyWorkoutAction {
        return workoutDao.getDailyWorkout(user.uid, workoutId).filter { it.value.actionId == workoutId }.map { it.toPair().toDailyWorkoutAction() }.first()
    }

    override suspend fun addWorkoutAction(user: User, action: DailyWorkoutAction) {
        val workout = action.toDbEntity(user.uid)
        workoutDao.addDailyWorkoutAction(workout)
        lastWorkout = action
    }

    override suspend fun deleteWorkoutAction(user: User, action: DailyWorkoutAction) {
        workoutDao.deleteDailyWorkoutAction(action.toDbEntity(user.uid))
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