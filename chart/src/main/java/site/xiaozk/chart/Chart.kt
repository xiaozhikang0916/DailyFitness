package site.xiaozk.chart

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import site.xiaozk.chart.internal.AxisValue
import site.xiaozk.chart.internal.ChartPoint
import site.xiaozk.chart.point.Line
import site.xiaozk.chart.property.AxisProperty
import site.xiaozk.chart.property.ChartColors
import site.xiaozk.chart.property.ChartDefault
import site.xiaozk.chart.property.ChartTextStyle
import kotlin.math.roundToInt

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/4/5
 */

@Composable
fun LineChart(
    line: Line,
    modifier: Modifier = Modifier,
    axisProperty: AxisProperty = AxisProperty(),
    colors: ChartColors = ChartDefault.colors(),
    textStyle: ChartTextStyle = ChartDefault.style(),
) {

    val sortedLine = remember(line) {
        line.points.distinct().sorted()
    }
    val points = remember(sortedLine) {
        mutableStateListOf<ChartPoint>()
    }
    val path = remember(points) {
        Path()
    }
    val yAxisPadding = remember {
        mutableStateOf(0)
    }
    val xAxisPadding = remember {
        mutableStateOf(0)
    }
    val yAxisValueLabel = remember(line) {
        line.points.associate {
            it.yValue to AxisValue(it.yValue, it.yDisplay)
        }
    }
    val xAxisValueLabel = remember(line) {
        line.points.associate {
            it.xValue to AxisValue(it.xValue, it.xDisplay)
        }
    }
    val yValueMap = remember(yAxisValueLabel) {
        mutableMapOf<Float, Int>()
    }
    val xValueMap = remember(xAxisValueLabel) {
        mutableMapOf<Float, Int>()
    }
    SubcomposeLayout(modifier = modifier.drawWithCache {
        onDrawBehind {
            // background of chart
            drawRect(color = colors.backgroundColor)

            // y axis
            drawLine(
                color = colors.axisColor,
                start = Offset(yAxisPadding.value.toFloat(), 0f),
                end = Offset(yAxisPadding.value.toFloat(), size.height),
                strokeWidth = axisProperty.axisWidth.toPx()
            )

            // x axis
            drawLine(
                color = colors.axisColor,
                start = Offset(0f, size.height - xAxisPadding.value.toFloat()),
                end = Offset(size.width, size.height - xAxisPadding.value.toFloat()),
                strokeWidth = axisProperty.axisWidth.toPx()
            )

            // draw line path
            drawPath(
                path = path,
                color = colors.lineColor,
                style = Stroke(width = axisProperty.lineWidth.toPx()),
            )
        }
    }) { c ->
        if (sortedLine.isEmpty()) {
            // fast return if no point needs to be drawn
            return@SubcomposeLayout layout(c.maxWidth, c.maxHeight) {}
        }
        val yLabel =
            yAxisValueLabel.mapValues {
                subcompose("y-${it.key}") {
                    Text(
                        text = it.value.display,
                        modifier = Modifier.padding(axisProperty.axisLabelPadding),
                        style = textStyle.axisValueStyle,
                        color = colors.axisValueColor
                    )
                }.first()
            }
        val yLabelPlaceable = yLabel.mapValues {
            it.value.measure(Constraints())
        }
        yAxisPadding.value = yLabelPlaceable.maxOf {
            it.value.width
        }

        val xLabel = xAxisValueLabel.mapValues {
            subcompose("x-${it.key}") {
                Text(
                    text = it.value.display,
                    modifier = Modifier.padding(axisProperty.axisLabelPadding),
                    style = textStyle.axisValueStyle,
                    color = colors.axisValueColor
                )
            }.first()
        }
        val xLabelPlaceable = xLabel.mapValues {
            it.value.measure(Constraints())
        }
        xAxisPadding.value = xLabelPlaceable.maxOf {
            it.value.height
        }
        val availableY = c.maxHeight - xAxisPadding.value
        val availableX = c.maxWidth - yAxisPadding.value

        val maxY = axisProperty.yAxisValueRange.maxValue.mapValueTo(line)
        val minY = axisProperty.yAxisValueRange.minValue.mapValueTo(line)
        val maxX = axisProperty.xAxisValueRange.maxValue.mapValueTo(line)
        val minX = axisProperty.xAxisValueRange.minValue.mapValueTo(line)

        points.clear()
        line.points.map {
            val x =
                (it.xValue.mapInRange(minX, maxX) * availableX + yAxisPadding.value).roundToInt()
            val y = ((1 - it.yValue.mapInRange(minY, maxY)) * availableY).roundToInt()
            xValueMap[it.xValue] = x
            yValueMap[it.yValue] = y
            ChartPoint(x, y)
        }.let {
            points.addAll(it)
        }
        path.reset()
        points.firstOrNull()?.let {
            path.moveTo(it.x.toFloat(), it.y.toFloat())
        }
        points.drop(1).forEach {
            path.lineTo(it.x.toFloat(), it.y.toFloat())
        }

        layout(c.maxWidth, c.maxHeight) {
            var lastY: Int = Int.MAX_VALUE
            yLabelPlaceable.entries.forEach { (value, p) ->
                val y = yValueMap[value] ?: Int.MAX_VALUE
                if (
                    lastY - p.height.div(2) >= y
                ) {
                    val placeY = y - p.height.div(2)
                    p.place(yAxisPadding.value - p.width, placeY)
                    lastY = placeY
                }
            }
            var lastX: Int = Int.MIN_VALUE
            xLabelPlaceable.entries.forEach { (value, p) ->
                val x = xValueMap[value] ?: Int.MAX_VALUE
                if (
                    lastX + p.width.div(2) <= x
                ) {
                    val placeX = x - p.width.div(2)
                    p.place(placeX, c.maxHeight - xAxisPadding.value)
                    lastX = placeX + p.width
                }
            }
        }
    }
}

private fun Float.mapInRange(min: Float, max: Float): Float {
    return (this - min) / (max - min)
}