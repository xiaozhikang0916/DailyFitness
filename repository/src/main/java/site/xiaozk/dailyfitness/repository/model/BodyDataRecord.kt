package site.xiaozk.dailyfitness.repository.model

import androidx.annotation.FloatRange
import java.time.Instant

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
data class BodyDataRecord(
    val id: Int = 0,
    val instant: Instant = Instant.EPOCH,
    val weight: Float = 0f,
    val bustSize: Float = 0f,
    val waistSize: Float = 0f,
    val hipSize: Float = 0f,
    @FloatRange(from = 0.0, to = 100.0)
    val bodyFat: Float = 0f,
) {
    internal operator fun plus(other: BodyDataRecord): BodyDataRecord {
        return this.copy(
            weight = this.weight + other.weight,
            bustSize = this.bustSize + other.bustSize,
            waistSize = this.waistSize + other.waistSize,
            hipSize = this.hipSize + other.hipSize,
            bodyFat = this.bodyFat + other.bodyFat,
        )
    }

    internal operator fun div(d: Int): BodyDataRecord {
        return this.copy(
            weight = this.weight / d,
            bustSize = this.bustSize / d,
            waistSize = this.waistSize / d,
            hipSize = this.hipSize / d,
            bodyFat = this.bodyFat / d,
        )
    }
}


enum class BodyField(
    val label: String,
    val trailing: String,
    val property: (BodyDataRecord) -> Float,
    val fieldRange: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
) {
    Weight("体重", "kg", BodyDataRecord::weight),
    Bust("胸围", "cm", BodyDataRecord::bustSize),
    Waist("腰围", "cm", BodyDataRecord::waistSize),
    Hip("臀围", "cm", BodyDataRecord::hipSize),
    BodyFat("体脂率", "%", BodyDataRecord::bodyFat, 0f..100f);
}