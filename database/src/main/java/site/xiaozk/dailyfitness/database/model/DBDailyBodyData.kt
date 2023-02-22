package site.xiaozk.dailyfitness.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import java.time.Instant

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Entity(tableName = "daily_body_data")
data class DBDailyBodyData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recordTime: Instant,
    val userId: Int,
    val weight: Float,
    val bustSize: Float,
    val waistSize: Float,
    val hipSize: Float,
    val bodyFat: Float,
) {
    fun toRepoEntity(): BodyDataRecord {
        return BodyDataRecord(
            instant = recordTime,
            weight = weight,
            bustSize = bustSize,
            waistSize = waistSize,
            hipSize = hipSize,
            bodyFat = bodyFat,
        )
    }
}