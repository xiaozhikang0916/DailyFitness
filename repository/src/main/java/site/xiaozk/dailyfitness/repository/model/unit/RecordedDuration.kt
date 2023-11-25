package site.xiaozk.dailyfitness.repository.model.unit

import kotlinx.serialization.Serializable

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */
@Serializable
data class RecordedDuration(
    val duration: Float = 0f,
    val timeUnit: TimeUnit = TimeUnit.Sec,
) : Comparable<RecordedDuration> {
    companion object {
        val Zero = RecordedDuration()
    }
    fun uniform(): RecordedDuration {
        return if (this.timeUnit == TimeUnit.Sec) {
            this
        } else {
            RecordedDuration(this.duration * this.timeUnit.trans, TimeUnit.Sec)
        }
    }

    override fun compareTo(other: RecordedDuration): Int {
        return this.uniform().duration.compareTo(this.uniform().duration)
    }

    override fun toString(): String {
        return "$duration ${timeUnit.name}"
    }
}

enum class TimeUnit(val trans: Int) {
    Sec(1), Min(60)
}