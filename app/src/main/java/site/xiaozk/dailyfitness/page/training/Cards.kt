package site.xiaozk.dailyfitness.page.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import site.xiaozk.dailyfitness.calendar.date.Day
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutSummary

/**
 * @author: xiaozhikang
 * @create: 2023/4/2
 */

@Composable
fun DayWithWorkout(day: Day, workout: DailyWorkoutSummary? = null) {
    val container = if (day.isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val background = if (day.isToday) {
        Modifier.background(color = container, shape = CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .heightIn(max = 40.dp)
            .aspectRatio(1f, true)
            .then(background),
    ) {
        val alignment = if (workout != null) {
            Alignment.TopCenter
        } else {
            Alignment.Center
        }
        Text(text = day.date.dayOfMonth.toString(), modifier = Modifier.align(alignment))
        if (workout != null) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(16.dp)
            )
        }
    }


}

@Composable
fun HomeWorkoutStaticsCard(
    title: String,
    content: String,
    subContent: String,
    modifier: Modifier = Modifier,
    bottom: String = "",
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(152.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .padding(top = 12.dp)
                .padding(horizontal = 12.dp),
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = content,
                    modifier = Modifier
                        .alignByBaseline(),
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = subContent,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .alignByBaseline(),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
        if (bottom.isNotBlank()) {
            Text(
                text = bottom,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.End
            )
        }
    }
}