package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import site.xiaozk.dailyfitness.utils.getLocalDateFormatter
import site.xiaozk.dailyfitness.utils.getLocalDateTimeFormatter
import site.xiaozk.dailyfitness.widget.SmallChip
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * @author: xiaozhikang
 * @create: 2023/3/19
 */


@Composable
fun HomeTrainPartHeadCard(homeTrainPartPage: HomeTrainPartPage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = stringResource(id = R.string.title_train_part), style = MaterialTheme.typography.titleSmall)
                    Text(text = homeTrainPartPage.partCount.toString(), style = MaterialTheme.typography.displaySmall)
                }
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(color = MaterialTheme.colorScheme.outline)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = stringResource(id = R.string.title_train_action), style = MaterialTheme.typography.titleSmall)
                    Text(text = homeTrainPartPage.actionCount.toString(), style = MaterialTheme.typography.displaySmall)
                }
            }
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .wrapContentWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_part_train_count),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = homeTrainPartPage.partWorkoutCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Divider(modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_action_train_group_count),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = homeTrainPartPage.workoutCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
fun TrainPartCard(
    trainPartStaticPage: TrainPartStaticPage,
    modifier: Modifier = Modifier,
    isHead: Boolean = false,
    onClicked: (() -> Unit)? = null,
) {
    val shape: Shape = if (isHead) CardDefaults.shape else CardDefaults.outlinedShape
    val colors: CardColors = if (isHead) CardDefaults.cardColors() else CardDefaults.outlinedCardColors()
    val elevation: CardElevation = if (isHead) CardDefaults.cardElevation() else CardDefaults.outlinedCardElevation()
    val border: BorderStroke? = if (isHead) null else CardDefaults.outlinedCardBorder()
    Card(
        modifier = modifier.then(
            if (onClicked != null) {
                Modifier
                    .clip(shape)
                    .clickable(onClick = onClicked)
            } else {
                Modifier
            }
        ), shape = shape,
        colors = colors,
        elevation = elevation,
        border = border
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = trainPartStaticPage.trainPart.partName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = stringResource(R.string.train_part_count_of_action, trainPartStaticPage.actionCount),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(IntrinsicSize.Max),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .width(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_part_train_count),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = trainPartStaticPage.partWorkoutCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Divider(modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outline)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.label_action_train_group_count),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        text = trainPartStaticPage.workoutCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
fun TrainActionCard(
    actionPage: TrainActionStaticPage,
    modifier: Modifier = Modifier,
    dateFormatter: DateTimeFormatter = getLocalDateFormatter().withZone(ZoneId.systemDefault()),
    isHead: Boolean = false,
    onClicked: (() -> Unit)? = null,
) {
    val action = actionPage.action
    val shape: Shape = if (isHead) CardDefaults.shape else CardDefaults.outlinedShape
    val colors: CardColors = if (isHead) CardDefaults.cardColors() else CardDefaults.outlinedCardColors()
    val elevation: CardElevation = if (isHead) CardDefaults.cardElevation() else CardDefaults.outlinedCardElevation()
    val border: BorderStroke? = if (isHead) null else CardDefaults.outlinedCardBorder()
    Card(
        modifier = modifier.then(
            if (onClicked != null) {
                Modifier
                    .clip(shape)
                    .clickable(onClick = onClicked)
            } else {
                Modifier
            }
        ),
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = action.actionName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(text = stringResource(R.string.action_train_group_count, actionPage.workoutCount), style = MaterialTheme.typography.titleSmall)

                }
                Row(
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (action.isWeightedAction) {
                        SmallChip(label = { Text(text = stringResource(R.string.label_train_action_type_weighted)) })
                    }
                    if (action.isTimedAction) {
                        SmallChip(label = { Text(text = stringResource(R.string.label_train_action_type_timed)) })
                    }
                    if (action.isCountedAction) {
                        SmallChip(label = { Text(text = stringResource(R.string.label_train_action_type_counted)) })
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(IntrinsicSize.Max)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                actionPage.maxList.entries.forEachIndexed { index, dailyWorkoutAction ->
                    if (index > 0) {
                        Divider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text = dailyWorkoutAction.key,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = dateFormatter.format(dailyWorkoutAction.value),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrainActionWorkoutCard(
    workout: DailyWorkoutAction,
    modifier: Modifier = Modifier,
    formatter: DateTimeFormatter = getLocalDateTimeFormatter(Locale.getDefault()).withZone(ZoneId.systemDefault()),
    onCardLongClick: (DailyWorkoutAction) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .combinedClickable(onLongClick = { onCardLongClick(workout) }) { }
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatter.format(workout.instant),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )

        val string = buildAnnotatedString {
            append(workout.displayText.joinToString(separator = " "))
            if (workout.note.isNotBlank()) {
                append('\n')
                pushStyle(MaterialTheme.typography.bodySmall.toSpanStyle())
                append(workout.note)
                pop()
            }
        }
        Text(
            text = string,
            modifier = Modifier.weight(1f, false),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            textAlign = TextAlign.End,
        )
    }
}