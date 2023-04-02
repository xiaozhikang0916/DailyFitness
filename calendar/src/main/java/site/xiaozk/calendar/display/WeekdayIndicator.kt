package site.xiaozk.calendar.display

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale

/**
 * @author: xiaozhikang
 * @create: 2023/4/1
 */

@Composable
fun WeekdayIndicator(
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    dateTimeFormatter: DateTimeFormatter = DateTimeFormatterBuilder().appendText(ChronoField.DAY_OF_WEEK, TextStyle.NARROW).toFormatter(Locale.getDefault()),
) {
    Row(modifier = Modifier.fillMaxWidth().height(DefaultCalendarRowHeight)) {
        (0..6).map {
            firstDayOfWeek.plus(it.toLong())
        }.forEach {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = dateTimeFormatter.format(it),
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
        }
    }
}