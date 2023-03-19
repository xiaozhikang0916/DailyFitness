package site.xiaozk.dailyfitness.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
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
    fun getAllTrainPart(): Flow<List<DBTrainPart>>

    @Query("SELECT * FROM train_part LEFT JOIN train_action ON train_part.id = train_action.partId WHERE train_part.id = :trainPartId")
    fun getAllTrainActionOnPart(trainPartId: Int): Flow<Map<DBTrainPart, List<DBTrainAction>>>

    @Query("SELECT * FROM train_part LEFT JOIN train_action ON train_part.id = train_action.partId")
    fun getAllTrainPartWithAction(): Flow<Map<DBTrainPart, List<DBTrainAction>?>>

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

}