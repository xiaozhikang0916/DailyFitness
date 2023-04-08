package site.xiaozk.dailyfitness.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.database.model.DBDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.DBTrainAction

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
            SELECT * from daily_train_action 
            JOIN train_action ON usingActionId = train_action.id 
            WHERE userId = :userId 
            AND actionTime > :fromTimestampMilli 
            AND actionTime < :toTimestampMilli
        """
    )
    fun getDailyWorkoutActions(userId: Int, fromTimestampMilli: Long, toTimestampMilli: Long): Flow<Map<DBTrainAction, List<DBDailyWorkoutAction>>>


    @Insert
    suspend fun addDailyWorkoutAction(dailyTrainAction: DBDailyWorkoutAction)

    @Delete
    suspend fun deleteDailyWorkoutAction(dailyTrainAction: DBDailyWorkoutAction)

    @Query("SELECT * FROM daily_train_action LEFT JOIN train_action on usingActionId = train_action.id WHERE userId = :userId ORDER BY actionTime DESC LIMIT 1")
    suspend fun getLatestWorkout(userId: Int): Map<DBTrainAction, DBDailyWorkoutAction>
}