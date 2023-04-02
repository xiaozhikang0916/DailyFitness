package site.xiaozk.dailyfitness.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.database.model.DBDailyBodyData
import site.xiaozk.dailyfitness.database.model.DBDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.DBTrainAction
import site.xiaozk.dailyfitness.database.utils.getEndEpochMillis
import site.xiaozk.dailyfitness.database.utils.getStartEpochMillis
import site.xiaozk.dailyfitness.repository.model.User
import java.time.LocalDate

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Dao
interface DailyDao {
    @Insert
    suspend fun addDailyPersonData(data: DBDailyBodyData)

    @Update
    suspend fun updateDailyPersonData(data: DBDailyBodyData)

    @Delete
    suspend fun deleteDailyPersonData(data: DBDailyBodyData)

    @Query("SELECT * from daily_body_data WHERE userId = :userId AND recordTime > :fromTimestampMilli AND recordTime < :toTimestampMilli")
    fun getPersonDailyDataFlow(userId: Int, fromTimestampMilli: Long, toTimestampMilli: Long): Flow<List<DBDailyBodyData>>

    fun getPersonDailyDataFlow(user: User, from: LocalDate, to: LocalDate): Flow<List<DBDailyBodyData>> {
        return getPersonDailyDataFlow(user.uid, from.getStartEpochMillis(), to.getEndEpochMillis())
    }

    /**
     * get the last body record of user [userId] with specific [column] is not null
     */
    @Query("SELECT * from daily_body_data WHERE userId = :userId AND :column IS NOT NULL ORDER BY recordTime DESC LIMIT 1")
    suspend fun getLastBodyDataWithColumn(userId: Int, column: String): DBDailyBodyData?

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

}