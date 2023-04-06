package site.xiaozk.dailyfitness.page.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import site.xiaozk.chart.LineChart
import site.xiaozk.chart.point.Line
import site.xiaozk.chart.point.PointHolder
import site.xiaozk.chart.property.AxisProperty
import site.xiaozk.dailyfitness.theme.DailyFitnessTheme
import kotlin.random.Random

/**
 * @author: xiaozhikang
 * @create: 2023/4/6
 */
@Preview
@Composable
fun PreviewChart() {
    DailyFitnessTheme {
        val points = (0..100).associate {
            it.toFloat() to (it * Random.nextDouble(0.0, 2.0)).toFloat()
        }.map {
            PointHolder(xValue = it.key, yValue = it.value)
        }
        LineChart(
            line = Line(points),
            modifier = Modifier.fillMaxWidth().height(200.dp),
            axisProperty = AxisProperty(axisLabelPadding = PaddingValues(all = 4.dp))
        )
    }
}