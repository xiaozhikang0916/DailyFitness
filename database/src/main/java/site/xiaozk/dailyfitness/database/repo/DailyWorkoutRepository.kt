package site.xiaozk.dailyfitness.database.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.map
import site.xiaozk.dailyfitness.database.dao.BodyDao
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.dao.WorkoutDao
import site.xiaozk.dailyfitness.database.model.toDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.toDbEntity
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
import site.xiaozk.dailyfitness.repository.model.User
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
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

    override fun getMonthWorkoutStatic(user: User, month: YearMonth): Flow<MonthWorkoutStatic> {
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

    override fun getHomeWorkoutStatics(user: User, month: YearMonth): Flow<HomeWorkoutStatic> {
        val weightFlow = bodyDao.getLastBodyDataWithWeight(userId = user.uid).map { it?.let { BodyStatic(it.recordTime, it.weight) } }
        val bustSizeFlow = bodyDao.getLastBodyDataWithBustSize(userId = user.uid).map { it?.let { BodyStatic(it.recordTime, it.bustSize) } }
        val waistSizeFlow = bodyDao.getLastBodyDataWithWaistSize(userId = user.uid).map { it?.let { BodyStatic(it.recordTime, it.waistSize) } }
        val hipSizeFlow = bodyDao.getLastBodyDataWithHipSize(userId = user.uid).map { it?.let { BodyStatic(it.recordTime, it.hipSize) } }
        val bodyFatFlow = bodyDao.getLastBodyDataWithBodyFat(userId = user.uid).map { it?.let { BodyStatic(it.recordTime, it.bodyFat) } }
        val staticFlow = combineTransform(
            weightFlow,
            bustSizeFlow,
            waistSizeFlow,
            hipSizeFlow,
            bodyFatFlow
        ) { (weight, bustSize, waistSize, hipSize, bodyFat) ->
            emit(DataHolder(weight, bustSize, waistSize, hipSize, bodyFat))
        }
        return getMonthWorkoutStatic(user, month).combineTransform(
            staticFlow,
        ) { it, (weight, bustSize, waistSize, hipSize, bodyFat) ->
            emit(
                HomeWorkoutStatic(
                    monthStatic = it,
                    weight = weight,
                    bustSize = bustSize,
                    waistSize = waistSize,
                    hipSize = hipSize,
                    bodyFat = bodyFat,
                )
            )
        }
    }

    override fun getWorkoutDayList(
        user: User,
        from: LocalDate,
        to: LocalDate,
    ): Flow<DailyWorkoutMap> {
        return workoutDao.getDailyWorkoutActions(
            user.uid, from.getStartEpochMillis(), to.getEndEpochMillis()
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
        return workoutDao.getDailyWorkout(user.uid, workoutId)
            .filter { it.value.actionId == workoutId }.map { it.toPair().toDailyWorkoutAction() }
            .first()
    }

    override suspend fun addWorkoutAction(user: User, action: DailyWorkoutAction) {
        val workout = action.toDbEntity(user.uid)
        workoutDao.addDailyWorkoutAction(workout)
    }

    override suspend fun deleteWorkoutAction(user: User, action: DailyWorkoutAction) {
        workoutDao.deleteDailyWorkoutAction(action.toDbEntity(user.uid))
    }

    override suspend fun getLastWorkout(user: User, date: LocalDate, zoneId: ZoneId): DailyWorkoutAction? {
        return workoutDao.getLatestWorkout(user.uid).entries.firstOrNull()
            ?.takeIf { it.value.actionTime.atZone(zoneId).toLocalDate() == date }?.toPair()
            ?.toDailyWorkoutAction()
    }
}

private data class DataHolder(
    val weight: BodyStatic? = null,
    val bustSize: BodyStatic? = null,
    val waistSize: BodyStatic? = null,
    val hipSize: BodyStatic? = null,
    val bodyFat: BodyStatic? = null,
)