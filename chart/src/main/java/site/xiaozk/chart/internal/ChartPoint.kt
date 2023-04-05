package site.xiaozk.chart.internal

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/4/5
 */

@Immutable
class ChartPoint(
    val x: Int,
    val y: Int,
) {
    constructor(x: Float, y: Float) : this(x.roundToInt(), y.roundToInt())
}

@Immutable
class AxisValue(
    val value: Float,
    val display: String,
): Comparable<AxisValue> {
    override fun compareTo(other: AxisValue): Int {
        return value.compareTo(other.value)
    }
}

