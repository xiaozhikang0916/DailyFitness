package site.xiaozk.dailyfitness.repository.model

import android.content.Context
import androidx.annotation.FloatRange
import androidx.annotation.StringRes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import site.xiaozk.dailyfitness.repository.R

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
    @StringRes val labelRes: Int,
    @StringRes val trailingRes: Int,
    val property: (BodyDataRecord) -> Float,
    val fieldRange: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
) {
    Weight(R.string.label_body_weight, R.string.label_weight_unit_kg, BodyDataRecord::weight),
    Bust(R.string.label_bust_size, R.string.label_length_unit_cm, BodyDataRecord::bustSize),
    Waist(R.string.label_waist_size, R.string.label_length_unit_cm, BodyDataRecord::waistSize),
    Hip(R.string.label_hip_size, R.string.label_length_unit_cm, BodyDataRecord::hipSize),
    BodyFat(R.string.label_body_fat, R.string.label_count_unit_percentage, BodyDataRecord::bodyFat, 0f..100f);

    fun getLabel(context: Context): String = context.getString(labelRes)

    fun getTrailing(context: Context): String = context.getString(trailingRes)
}