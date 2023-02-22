package site.xiaozk.dailyfitness.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import site.xiaozk.dailyfitness.database.model.DBUser

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Dao
interface UserDao {
    @Insert
    suspend fun createUser(user: DBUser)

    @Query("SELECT * from user")
    suspend fun getAllUsers(): List<DBUser>

    @Query("SELECT * from user where user.uid == :id")
    suspend fun getUserById(id: Int): DBUser?

}