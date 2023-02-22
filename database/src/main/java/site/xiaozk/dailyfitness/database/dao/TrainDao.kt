package site.xiaozk.dailyfitness.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

}