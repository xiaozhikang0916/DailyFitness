package site.xiaozk.dailyfitness.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.HomeWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.User
import site.xiaozk.dailyfitness.repository.model.now
import kotlin.time.Clock

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
interface IDailyWorkoutRepository {
    fun getMonthWorkoutStatic(
        user: User,
        month: YearMonth = YearMonth.now()
    ): Flow<MonthWorkoutStatic>

    fun getHomeWorkoutStatics(
        user: User,
        month: YearMonth = YearMonth.now()
    ): Flow<HomeWorkoutStatic>

    /** Days ordered oldest-first (chronological), each day's records ordered oldest-first. */
    fun getWorkoutDayList(user: User, from: LocalDate, to: LocalDate): Flow<List<DailyWorkout>>

    /** Days ordered oldest-first (chronological), each day's records ordered oldest-first. */
    fun getAllWorkoutDayList(user: User): Flow<List<DailyWorkout>>

    fun getWorkoutOfDayFlow(user: User, day: LocalDate): Flow<DailyWorkout?> {
        return getWorkoutDayList(user, day, day).map { workouts -> workouts.firstOrNull { it.date == day } }
    }

    suspend fun getWorkout(user: User, workoutId: Int): DailyWorkoutAction

    suspend fun addWorkoutAction(user: User, action: DailyWorkoutAction)

    suspend fun deleteWorkoutAction(user: User, action: DailyWorkoutAction)

    suspend fun getLastWorkout(
        user: User,
        date: LocalDate = Clock.System.now().toLocalDateTime(
            TimeZone.currentSystemDefault()
        ).date,
        zoneId: TimeZone = TimeZone.currentSystemDefault()
    ): DailyWorkoutAction?
}