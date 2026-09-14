package site.xiaozk.dailyfitness.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import site.xiaozk.dailyfitness.database.model.DBDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.DBDailyWorkoutRecord
import site.xiaozk.dailyfitness.database.model.DBTrainAction
import site.xiaozk.dailyfitness.database.model.toDailyWorkoutList
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import kotlin.time.Instant

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Dao
interface WorkoutDao {
    @Query("SELECT * FROM daily_train_action LEFT JOIN train_action on usingActionId = train_action.id WHERE userId = :userId AND actionId = :workoutId")
    suspend fun getDailyWorkout(userId: Int, workoutId: Int): Map<DBTrainAction, DBDailyWorkoutAction>

    @Query(
        """
            SELECT daily_train_action.*, train_action.*, train_part.part_name AS partName
            from daily_train_action 
            JOIN train_action ON usingActionId = train_action.id 
            JOIN train_part ON train_action.partId = train_part.id 
            WHERE userId = :userId 
            AND actionTime > :from 
            AND actionTime < :to
            ORDER BY actionTime ASC, actionId ASC
        """
    )
    fun getWorkoutDayRecords(userId: Int, from: Instant, to: Instant): Flow<List<DBDailyWorkoutRecord>>

    @Query(
        """
            SELECT daily_train_action.*, train_action.*, train_part.part_name AS partName
            from daily_train_action 
            JOIN train_action ON usingActionId = train_action.id 
            JOIN train_part ON train_action.partId = train_part.id 
            WHERE userId = :userId
            ORDER BY actionTime ASC, actionId ASC
        """
    )
    fun getAllWorkoutRecords(userId: Int): Flow<List<DBDailyWorkoutRecord>>


    @Insert
    suspend fun addDailyWorkoutAction(dailyTrainAction: DBDailyWorkoutAction)

    @Delete
    suspend fun deleteDailyWorkoutAction(dailyTrainAction: DBDailyWorkoutAction)

    @Query("SELECT * FROM daily_train_action LEFT JOIN train_action on usingActionId = train_action.id WHERE userId = :userId ORDER BY actionTime DESC LIMIT 1")
    suspend fun getLatestWorkout(userId: Int): Map<DBTrainAction, DBDailyWorkoutAction>
}

/**
 * New ordered API: strictly oldest-first (chronological) list of [DailyWorkout]s for the
 * given range.
 *
 * Implemented as an extension because Room cannot map a query straight onto the
 * aggregated [DailyWorkout] model.
 */
fun WorkoutDao.getWorkoutDayList(
    userId: Int,
    from: Instant,
    to: Instant,
): Flow<List<DailyWorkout>> =
    getWorkoutDayRecords(userId, from, to)
        .map { it.toDailyWorkoutList() }

/**
 * New ordered API: strictly oldest-first (chronological) list of every [DailyWorkout]
 * of the user.
 */
fun WorkoutDao.getAllDailyWorkoutActions(userId: Int): Flow<List<DailyWorkout>> =
    getAllWorkoutRecords(userId).map { it.toDailyWorkoutList() }