package site.xiaozk.dailyfitness.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord

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
            id = id,
            instant = recordTime,
            weight = weight,
            bustSize = bustSize,
            waistSize = waistSize,
            hipSize = hipSize,
            bodyFat = bodyFat,
        )
    }
}

fun BodyDataRecord.toDbEntity(userId: Int = 0): DBDailyBodyData {
    return DBDailyBodyData(
        id = this.id,
        recordTime = this.instant,
        userId = userId,
        weight = this.weight,
        bustSize = this.bustSize,
        waistSize = this.waistSize,
        hipSize = this.hipSize,
        bodyFat = this.bodyFat
    )
}