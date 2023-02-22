package site.xiaozk.dailyfitness.repository.model

import androidx.annotation.FloatRange
import java.time.Instant

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
data class BodyDataRecord(
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

fun Collection<BodyDataRecord>.average(): BodyDataRecord {
    val count = this.size
    return if (count == 1) {
        this.first()
    } else {
        this.fold(BodyDataRecord(Instant.now(), 0f, 0f, 0f, 0f, 0f)) { acc, data ->
            acc + data
        } / count
    }
}