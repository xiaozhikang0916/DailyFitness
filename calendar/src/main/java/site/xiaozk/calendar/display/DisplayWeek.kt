package site.xiaozk.calendar.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import site.xiaozk.calendar.date.Day
import site.xiaozk.calendar.date.Week

/**
 * @author: xiaozhikang
 * @create: 2023/4/1
 */

@Composable
fun DisplayWeek(
    displayWeek: Week,
    modifier: Modifier = Modifier,
    showOverlappingDays: Boolean = true,
    day: @Composable RowScope.(Day) -> Unit = {
        DisplayDay(
            displayDay = it,
            modifier = Modifier,
            showOverlappingDays = showOverlappingDays
        )
    },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DefaultCalendarRowHeight)
    ) {

        displayWeek.days.forEach {
            val container = if (it.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            }
            val color = if (it.isToday) {
                contentColorFor(backgroundColor = container)
            } else if (it.isCurrentMonth) {
                LocalContentColor.current
            } else {
                LocalContentColor.current.copy(alpha = DisabledContentAlpha)
            }
            CompositionLocalProvider(LocalContentColor provides color) {
                day(it)
            }
        }
    }
}

@Composable
fun RowScope.DisplayDay(
    displayDay: Day,
    modifier: Modifier = Modifier,
    showOverlappingDays: Boolean = true,
) {
    val container = if (displayDay.isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val background = if (displayDay.isToday) {
        Modifier.background(color = container, shape = CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .heightIn(max = 40.dp)
                .aspectRatio(1f, true)
                .then(background),
            contentAlignment = Alignment.Center,
        ) {
            if (showOverlappingDays || displayDay.isCurrentMonth) {
                Text(text = displayDay.date.dayOfMonth.toString())
            }
        }
    }
}