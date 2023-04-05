package site.xiaozk.chart.property

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/4/5
 */
@Immutable
class ChartColors(
    val backgroundColor: Color,
    val lineColor: Color,
    val axisColor: Color = lineColor,
    val axisValueColor: Color,
    val onChartValueColor: Color
)

@Immutable
class ChartTextStyle(
    val axisValueStyle: TextStyle,
    val onChartValueStyle: TextStyle,
)

object ChartDefault {
    @Composable
    fun colors(
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        lineColor: Color = contentColorFor(backgroundColor = backgroundColor),
        axisColor: Color = lineColor,
        axisValueColor: Color = MaterialTheme.colorScheme.primary,
        onChartValueColor: Color = axisColor
    ) = ChartColors(
        backgroundColor = backgroundColor,
        lineColor = lineColor,
        axisColor = axisColor,
        axisValueColor = axisValueColor,
        onChartValueColor = onChartValueColor
    )

    @Composable
    fun style(
        axisValueStyle: TextStyle = MaterialTheme.typography.bodySmall,
        onChartValueStyle: TextStyle = MaterialTheme.typography.bodySmall,
    ) = ChartTextStyle(
        axisValueStyle = axisValueStyle,
        onChartValueStyle = onChartValueStyle
    )
}