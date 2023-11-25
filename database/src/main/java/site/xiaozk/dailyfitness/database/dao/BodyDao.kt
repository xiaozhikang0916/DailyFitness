package site.xiaozk.dailyfitness.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import site.xiaozk.dailyfitness.database.model.DBDailyBodyData
import site.xiaozk.dailyfitness.database.utils.getEndEpochMillis
import site.xiaozk.dailyfitness.database.utils.getStartEpochMillis
import site.xiaozk.dailyfitness.repository.model.User

/**
 * @author: xiaozhikang
 * @create: 2023/4/3
 */
@Dao
interface BodyDao {
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
     * get the last body record of user [userId] with weight is grater than 0
     */
    @Query("SELECT * from daily_body_data WHERE userId = :userId AND abs(weight - 0.0) > 0.0001 ORDER BY recordTime DESC LIMIT 1")
    fun getLastBodyDataWithWeight(userId: Int): Flow<DBDailyBodyData?>

    /**
     * get the last body record of user [userId] with bustSize is grater than 0
     */
    @Query("SELECT * from daily_body_data WHERE userId = :userId AND abs(bustSize - 0.0) > 0.0001 ORDER BY recordTime DESC LIMIT 1")
    fun getLastBodyDataWithBustSize(userId: Int): Flow<DBDailyBodyData?>

    /**
     * get the last body record of user [userId] with waistSize is grater than 0
     */
    @Query("SELECT * from daily_body_data WHERE userId = :userId AND abs(waistSize - 0.0) > 0.0001 ORDER BY recordTime DESC LIMIT 1")
    fun getLastBodyDataWithWaistSize(userId: Int): Flow<DBDailyBodyData?>

    /**
     * get the last body record of user [userId] with hipSize is grater than 0
     */
    @Query("SELECT * from daily_body_data WHERE userId = :userId AND abs(hipSize - 0.0) > 0.0001 ORDER BY recordTime DESC LIMIT 1")
    fun getLastBodyDataWithHipSize(userId: Int): Flow<DBDailyBodyData?>

    /**
     * get the last body record of user [userId] with bodyFat is grater than 0
     */
    @Query("SELECT * from daily_body_data WHERE userId = :userId AND abs(bodyFat - 0.0) > 0.0001 ORDER BY recordTime DESC LIMIT 1")
    fun getLastBodyDataWithBodyFat(userId: Int): Flow<DBDailyBodyData?>

}