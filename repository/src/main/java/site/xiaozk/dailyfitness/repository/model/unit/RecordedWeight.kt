package site.xiaozk.dailyfitness.repository.model.unit

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */
data class RecordedWeight(
    val weight: Float = 0f,
    val weightUnit: WeightUnit = WeightUnit.Kg,
) : Comparable<RecordedWeight> {
    companion object {
        val Zero = RecordedWeight()
    }

    fun uniform(): RecordedWeight {
        return if (weightUnit == WeightUnit.Kg) {
            this
        } else {
            RecordedWeight(this.weight * this.weightUnit.trans, WeightUnit.Kg)
        }
    }

    override fun compareTo(other: RecordedWeight): Int {
        return this.uniform().weight.compareTo(other.uniform().weight)
    }

    override fun toString(): String {
        return "$weight ${weightUnit.name}"
    }
}

enum class WeightUnit(val trans: Float) {
    Kg(1f), Lbs(0.45359f)
}