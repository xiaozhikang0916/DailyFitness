package site.xiaozk.dailyfitness.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.database.model.DBDailyBodyData
import site.xiaozk.dailyfitness.database.model.DBDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.DBTrainAction
import site.xiaozk.dailyfitness.database.model.DBTrainPart
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

    @Query("""
            SELECT * from daily_train_action 
            JOIN train_action ON usingActionId = train_action.id 
            WHERE userId = :userId 
            AND actionTime > :fromTimestampMilli 
            AND actionTime < :toTimestampMilli
        """)
    fun getDailyWorkoutActions(userId: Int, fromTimestampMilli: Long, toTimestampMilli: Long): Flow<Map<DBTrainAction, List<DBDailyWorkoutAction>>>

    @MapInfo(keyTable = "train_action", keyColumn = "actionID", valueTable = "train_part")
    @Query("SELECT train_action.id as actionID, train_part.* FROM train_part JOIN train_action ON train_action.partId = train_part.id WHERE train_action.id IN (:actionId) ")
    suspend fun getTrainPartOfAction(actionId: IntArray): Map<Int, DBTrainPart>


    @Insert
    suspend fun addDailyWorkoutAction(dailyTrainAction: DBDailyWorkoutAction)

    @Delete
    suspend fun deleteDailyWorkoutAction(dailyTrainAction: DBDailyWorkoutAction)

}