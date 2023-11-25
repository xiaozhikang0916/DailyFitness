package site.xiaozk.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek
import site.xiaozk.dailyfitness.calendar.date.Day
import site.xiaozk.dailyfitness.calendar.date.Month
import site.xiaozk.calendar.display.CalendarHeader
import site.xiaozk.calendar.display.DisplayDay
import site.xiaozk.calendar.display.DisplayWeek
import site.xiaozk.calendar.display.WeekdayIndicator
import site.xiaozk.dailyfitness.repository.model.YearMonth

/**
 * @author: xiaozhikang
 * @create: 2023/4/1
 */


@Composable
fun Calendar(
    displayMonth: Month,
    modifier: Modifier = Modifier,
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    showOverlappingDays: Boolean = true,
    onMonthChanged: ((YearMonth) -> Unit)? = null,
    showMonthNavigator: Boolean = onMonthChanged != null,
    calendarHeader: @Composable () -> Unit = {
        CalendarHeader(
            month = displayMonth.yearMonth,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
            showNavigator = showMonthNavigator,
            onMonthChanged = onMonthChanged
        )
    },
    weekdayIndicator: @Composable () -> Unit = {
        WeekdayIndicator(firstDayOfWeek)
    },
    onDayClick: (Day) -> Unit = {},
    displayDay: @Composable BoxScope.(Day) -> Unit = {
        DisplayDay(it, modifier = Modifier.clickable { onDayClick(it) })
    },
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    calendarHeader()
                }
                weekdayIndicator()
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                    LocalTextStyle provides MaterialTheme.typography.bodyLarge
                ) {
                    displayMonth.getWeeks(firstDayOfWeek).forEach {
                        DisplayWeek(
                            displayWeek = it,
                            modifier = Modifier.fillMaxWidth(),
                            showOverlappingDays = showOverlappingDays,
                            day = displayDay
                        )
                    }
                }
            }
        }
    }
}