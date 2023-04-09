package site.xiaozk.dailyfitness.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.database.model.DBDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.DBTrainAction
import site.xiaozk.dailyfitness.database.model.DBTrainPart

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Dao
interface TrainDao {
    @Query("SELECT * FROM train_part")
    suspend fun getAllTrainPart(): List<DBTrainPart>

    @Query("SELECT * FROM train_part WHERE train_part.id = :partId")
    fun getTrainPart(partId: Int): Flow<DBTrainPart>

    @Query("SELECT * FROM train_part LEFT JOIN train_action ON train_part.id = train_action.partId WHERE train_part.id = :trainPartId")
    fun getAllTrainActionOnPart(trainPartId: Int): Flow<Map<DBTrainPart, List<DBTrainAction>>>

    @Query("SELECT * FROM train_part LEFT JOIN train_action ON train_part.id = train_action.partId")
    fun getAllTrainPartWithAction(): Flow<Map<DBTrainPart, List<DBTrainAction>?>>

    @Query("SELECT train_action.*, daily_train_action.* FROM train_action LEFT JOIN daily_train_action ON train_action.id = daily_train_action.usingActionId JOIN train_part ON train_action.partId = train_part.id")
    fun getAllTrainActionWithWorkout(): Flow<Map<DBTrainAction, List<DBDailyWorkoutAction>>>

    @Query("SELECT train_action.*, daily_train_action.* FROM train_action LEFT JOIN daily_train_action ON train_action.id = daily_train_action.usingActionId JOIN train_part ON train_action.partId = train_part.id WHERE train_action.partId = :partId")
    fun getTrainActionWithWorkoutOfPart(partId: Int): Flow<Map<DBTrainAction, List<DBDailyWorkoutAction>>>

    @Query("SELECT * FROM train_action LEFT JOIN daily_train_action ON train_action.id = daily_train_action.usingActionId WHERE train_action.id = :actionId")
    fun getTrainActionWithWorkout(actionId: Int): Flow<Map<DBTrainAction, List<DBDailyWorkoutAction>>>

    @Insert
    suspend fun addTrainPart(trainPart: DBTrainPart)

    @Insert
    suspend fun addTrainAction(trainAction: DBTrainAction)

    @Query("SELECT * FROM train_action WHERE train_action.id = :actionId LIMIT 1")
    fun getTrainAction(actionId: Int): Flow<DBTrainAction>

    @Query("SELECT * FROM train_action JOIN train_part ON train_action.partId = train_part.id WHERE train_action.id = :actionId")
    fun getTrainActionWithPart(actionId: Int): Flow<Map<DBTrainAction, DBTrainPart>>

    @Delete
    suspend fun deleteTrainPart(trainPart: DBTrainPart)

    @Delete
    suspend fun deleteTrainAction(trainAction: DBTrainAction)

    @Update
    suspend fun updateTrainPart(trainPart: DBTrainPart)

    @Update
    suspend fun updateTrainAction(trainAction: DBTrainAction)


    /**
     * A helper query to map a group of train actions with id [actionId] to it's belonging train part
     */
    @MapInfo(keyTable = "train_action", keyColumn = "actionID", valueTable = "train_part")
    @Query("SELECT train_action.id as actionID, train_part.* FROM train_part JOIN train_action ON train_action.partId = train_part.id WHERE train_action.id IN (:actionId) ")
    suspend fun getTrainPartOfAction(actionId: IntArray): Map<Int, DBTrainPart>

}