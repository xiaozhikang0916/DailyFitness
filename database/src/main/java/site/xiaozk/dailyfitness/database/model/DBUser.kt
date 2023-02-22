package site.xiaozk.dailyfitness.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import site.xiaozk.dailyfitness.repository.model.User

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Entity(tableName = "user")
data class DBUser(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    val name: String = "",
) {
    fun toRepoEntity(): User {
        return User(uid, name)
    }
}
