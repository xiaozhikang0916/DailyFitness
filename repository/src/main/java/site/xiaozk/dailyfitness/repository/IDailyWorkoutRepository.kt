package site.xiaozk.dailyfitness.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import site.xiaozk.dailyfitness.repository.model.YearMonth
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutMap
import site.xiaozk.dailyfitness.repository.model.HomeWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.User

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

    fun getWorkoutDayList(user: User, from: LocalDate, to: LocalDate): Flow<DailyWorkoutMap>

    fun getWorkoutOfDayFlow(user: User, day: LocalDate): Flow<DailyWorkout?> {
        return getWorkoutDayList(user, day, day).map { it.trainedDate[day] }
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