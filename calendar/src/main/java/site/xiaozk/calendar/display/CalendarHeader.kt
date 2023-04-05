package site.xiaozk.calendar.display

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * @author: xiaozhikang
 * @create: 2023/4/1
 */

private val DefaultYeadMonthFormatter: DateTimeFormatter
    get() = DateTimeFormatterBuilder()
        .appendText(ChronoField.YEAR, TextStyle.FULL)
        .appendLiteral(" ")
        .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL)
        .toFormatter(Locale.getDefault())

@Composable
fun CalendarHeader(
    month: YearMonth,
    dateTimeFormatter: DateTimeFormatter = remember { DefaultYeadMonthFormatter },
    onMonthChanged: ((YearMonth) -> Unit)? = null,
    showNavigator: Boolean = onMonthChanged != null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dateTimeFormatter.format(month),
            modifier = Modifier
                .padding(start = 24.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showNavigator) {
            Row(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .padding(vertical = 4.dp)
            ) {
                IconButton(onClick = {
                    onMonthChanged?.invoke(month.minusMonths(1))
                }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Last month"
                    )
                }
                IconButton(onClick = {
                    onMonthChanged?.invoke(month.plusMonths(1))
                }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next month"
                    )
                }
            }
        }
    }
}