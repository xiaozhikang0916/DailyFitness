package site.xiaozk.chart.property

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import site.xiaozk.chart.point.Line

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/4/5
 */

@Immutable
class AxisProperty(
    val xAxisValueRange: AxisValueRange<XAxisValueMapper> = NormalXAxisRange,
    val yAxisValueRange: AxisValueRange<YAxisValueMapper> = NormalYAxisRange,
    val axisLabelPadding: PaddingValues = PaddingValues(),
    val axisWidth: Dp = Dp.Hairline,
    val lineWidth: Dp = Dp.Hairline,
)

/**
 * Describe how to determine a value of an axis
 * based on a given line
 * e.g. get the maximum and minimum values of an axis
 */
fun interface AxisValueMapper {
    fun mapValueTo(line: Line): Float
}

fun interface XAxisValueMapper : AxisValueMapper

fun interface YAxisValueMapper : AxisValueMapper

fun interface XYAxisValueMapper : XAxisValueMapper, YAxisValueMapper

val ZeroMapper = XYAxisValueMapper { 0f }

val MaxXOfLineMapper = XAxisValueMapper { it.maxX }

val MaxYOfLineMapper = YAxisValueMapper { it.maxY }

@Immutable
class AxisValueRange<X : AxisValueMapper>(
    val minValue: X,
    val maxValue: X,
)

val NormalXAxisRange = AxisValueRange(
    minValue = XAxisValueMapper {
        it.minX - ((it.maxX - it.minX) * 0.2f)
    },
    maxValue = XAxisValueMapper {
        it.maxX + ((it.maxX - it.minX) * 0.2f)
    }
)

val NormalYAxisRange = AxisValueRange(
    minValue = YAxisValueMapper {
        it.minY - ((it.maxY - it.minY) * 0.2f)
    },
    maxValue = YAxisValueMapper {
        it.maxY + ((it.maxY - it.minY) * 0.2f)
    }
)