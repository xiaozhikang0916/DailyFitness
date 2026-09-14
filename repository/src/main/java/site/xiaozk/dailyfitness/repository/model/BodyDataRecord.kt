package site.xiaozk.dailyfitness.repository.model

import androidx.annotation.FloatRange
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Serializable
data class BodyDataRecord(
    val id: Int = 0,
    val instant: Instant = Clock.System.now(),
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
    val property: (BodyDataRecord) -> Float,
    val fieldRange: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
) {
    Weight(BodyDataRecord::weight),
    Bust(BodyDataRecord::bustSize),
    Waist(BodyDataRecord::waistSize),
    Hip(BodyDataRecord::hipSize),
    BodyFat(BodyDataRecord::bodyFat, 0f..100f);
}