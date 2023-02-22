package site.xiaozk.dailyfitness.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.database.model.DBDailyBodyData
import site.xiaozk.dailyfitness.database.model.DBDailyTrainAction
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
    fun getDailyTrainingActions(userId: Int, fromTimestampMilli: Long, toTimestampMilli: Long): Flow<Map<DBTrainAction, List<DBDailyTrainAction>>>

    @MapInfo(keyTable = "train_action", keyColumn = "actionID", valueTable = "train_part")
    @Query("SELECT train_action.id as actionID, train_part.* FROM train_part JOIN train_action ON train_action.partId = train_part.id WHERE train_action.id IN (:actionId) ")
    suspend fun getTrainPartOfAction(actionId: IntArray): Map<Int, DBTrainPart>

    //TODO use multimap instead of multi query
//    @Query("""
//        SELECT train_part.*, daily.* FROM (
//            SELECT train_action.*, daily_train_action.* from daily_train_action
//            JOIN train_action ON usingActionId = train_action.id
//            WHERE userId = :userId
//            AND actionTime > :fromTimestampMilli
//            AND actionTime < :toTimestampMilli
//            ) as daily
//        JOIN train_part ON partId = train_part.id
//        """)
//    fun getDailyTrainingDataFlow(userId: Int, fromTimestampMilli: Long, toTimestampMilli: Long): Flow<Map<DBTrainPart, List<Map<DBTrainAction, List<DBDailyTrainAction>>>>>

//    fun getDailyTrainingDataFlow(user: User, from: LocalDate, to: LocalDate) =
//        getDailyTrainingDataFlow(user.uid, from.getStartEpochMillis(), to.getEndEpochMillis())


    @Insert
    suspend fun addDailyTrainAction(dailyTrainAction: DBDailyTrainAction)

}