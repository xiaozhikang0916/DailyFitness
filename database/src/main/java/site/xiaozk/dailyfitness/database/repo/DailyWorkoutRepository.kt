package site.xiaozk.dailyfitness.database.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import site.xiaozk.dailyfitness.database.dao.BodyDao
import site.xiaozk.dailyfitness.database.dao.WorkoutDao
import site.xiaozk.dailyfitness.database.dao.getAllDailyWorkoutActions
import site.xiaozk.dailyfitness.database.dao.getWorkoutDayList
import site.xiaozk.dailyfitness.database.model.toDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.toDailyWorkoutList
import site.xiaozk.dailyfitness.database.model.toDbEntity
import site.xiaozk.dailyfitness.database.utils.getEndInstant
import site.xiaozk.dailyfitness.database.utils.getStartInstant
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.model.BodyStatic
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutSummary
import site.xiaozk.dailyfitness.repository.model.HomeWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.User
import site.xiaozk.dailyfitness.repository.model.WorkoutDaySummaryMap
import java.util.TreeMap
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */
class DailyWorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val bodyDao: BodyDao,
) : IDailyWorkoutRepository {

    override fun getMonthWorkoutStatic(user: User, month: YearMonth): Flow<MonthWorkoutStatic> {
        return workoutDao.getWorkoutDayRecords(
            user.uid,
            month.firstDay.getStartInstant(),
            month.lastDay.getEndInstant(),
        ).map { records ->
            val workouts = records.toDailyWorkoutList()
            MonthWorkoutStatic(
                month = month,
                workoutDays = WorkoutDaySummaryMap(
                    TreeMap(workouts.associate { it.date to DailyWorkoutSummary(it) })
                ),
            )
        }
    }

    override fun getHomeWorkoutStatics(user: User, month: YearMonth): Flow<HomeWorkoutStatic> {
        val weightFlow = bodyDao.getLastBodyDataWithWeight(userId = user.uid)
            .map { it?.let { BodyStatic(it.recordTime, it.weight) } }
        val bustSizeFlow = bodyDao.getLastBodyDataWithBustSize(userId = user.uid)
            .map { it?.let { BodyStatic(it.recordTime, it.bustSize) } }
        val waistSizeFlow = bodyDao.getLastBodyDataWithWaistSize(userId = user.uid)
            .map { it?.let { BodyStatic(it.recordTime, it.waistSize) } }
        val hipSizeFlow = bodyDao.getLastBodyDataWithHipSize(userId = user.uid)
            .map { it?.let { BodyStatic(it.recordTime, it.hipSize) } }
        val bodyFatFlow = bodyDao.getLastBodyDataWithBodyFat(userId = user.uid)
            .map { it?.let { BodyStatic(it.recordTime, it.bodyFat) } }
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
    ): Flow<List<DailyWorkout>> {
        return workoutDao.getWorkoutDayList(
            user.uid, from.getStartInstant(), to.getEndInstant()
        )
    }

    override fun getAllWorkoutDayList(user: User): Flow<List<DailyWorkout>> {
        return workoutDao.getAllDailyWorkoutActions(user.uid)
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

    override suspend fun getLastWorkout(
        user: User,
        date: LocalDate,
        zoneId: TimeZone
    ): DailyWorkoutAction? {
        return workoutDao.getLatestWorkout(user.uid).entries.firstOrNull()
            ?.takeIf { it.value.actionTime.toLocalDateTime(zoneId).date == date }?.toPair()
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
