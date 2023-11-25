package site.xiaozk.calendar.display

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import site.xiaozk.dailyfitness.repository.model.YearMonth

/**
 * @author: xiaozhikang
 * @create: 2023/4/1
 */


@Composable
fun CalendarHeader(
    month: site.xiaozk.dailyfitness.repository.model.YearMonth,
    modifier: Modifier = Modifier,
    onMonthChanged: ((site.xiaozk.dailyfitness.repository.model.YearMonth) -> Unit)? = null,
    showNavigator: Boolean = onMonthChanged != null,
) {
    Row(
        modifier = modifier.height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = month.toString(),
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showNavigator) {
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
            ) {
                IconButton(onClick = {
                    onMonthChanged?.invoke(
                        site.xiaozk.dailyfitness.repository.model.YearMonth(
                            year = month.year,
                            month = month.month.minus(1)
                        )
                    )
                }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Last month"
                    )
                }
                IconButton(onClick = {
                    onMonthChanged?.invoke(
                        site.xiaozk.dailyfitness.repository.model.YearMonth(
                            year = month.year,
                            month = month.month.plus(1)
                        )
                    )
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