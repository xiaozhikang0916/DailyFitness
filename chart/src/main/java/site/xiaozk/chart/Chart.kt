package site.xiaozk.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.DecimalFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.log10
import kotlin.math.pow

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/4/5
 */

@Composable
fun LineChart(
    line: BodyChartLine,
    displayMonth: YearMonth,
    modifier: Modifier = Modifier,
) {
    val format = remember {
        DecimalFormat("0.00")
    }
    ProvideChartStyle(
        m3ChartStyle(
            axisGuidelineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        )
    ) {
        Chart(
            chart = lineChart(
                axisValuesOverrider = AxisValuesOverrider.fixed(
                    minY = line.minY.displayFloor().coerceAtLeast(0f),
                    maxY = line.maxY.displayCeil(),
                    minX = 1f,
                    maxX = displayMonth.lengthOfMonth().toFloat(),
                )
            ),
            modifier = modifier,
            model = line.chartEntryModel,
            startAxis = startAxis(
                valueFormatter = { value, _ ->
                    format.format(value)
                }
            ),
            bottomAxis = bottomAxis(),
        )
    }
}

@JvmInline
value class BodyChartLine(
    val line: List<BodyChartPoint>,
) {
    val minY: Float
        get() = line.minByOrNull { it.value }?.value ?: 0f
    val maxY: Float
        get() = line.maxByOrNull { it.value }?.value ?: 0f
    val chartEntryModel: ChartEntryModel
        get() = entryModelOf(
            line.groupBy { it.date }
                .toSortedMap()
                .mapValues { it.value.filter { it.value > 0 }.minByOrNull { it.value } }
                .mapNotNull { kv ->
                    kv.value?.takeIf { it.value > 0 }?.let {
                        FloatEntry(kv.key.toFloat(), it.value)
                    }
                }
        )
}

data class BodyChartPoint(
    val instant: Instant,
    val value: Float,
) {
    val date: Int
        get() = instant.atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth
}

internal fun Float.displayFloor(): Float {
    if (this == 0f) return 0f
    val digits = log10(this).toInt()
    return this - this.mod(10f.pow(digits))
}

internal fun Float.displayCeil(): Float {
    if (this == 0f) return 0f
    val digits = log10(this).toInt()
    return (this.div(10f.pow(digits)) + 1).toInt().times(10f.pow(digits))
}