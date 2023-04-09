package site.xiaozk.dailyfitness.page.body

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.mapNotNull
import site.xiaozk.calendar.display.CalendarHeader
import site.xiaozk.chart.LineChart
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.HomepageScaffoldState
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.SnackbarData
import site.xiaozk.dailyfitness.nav.SnackbarStatus
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.BodyField
import site.xiaozk.dailyfitness.utils.getLocalDateTimeFormatter
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/25
 */

@Composable
fun BodyDetailPage() {
    val viewModel: BodyViewModel = hiltViewModel()

    val appScaffoldViewModel: AppScaffoldViewModel = localAppScaffoldViewModel()
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            HomepageScaffoldState(
                title = "身体数据",
            )
        )
    }
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.snackbarFlow.emitAll(
            viewModel.deleteAction.mapNotNull {
                when (it) {
                    ActionStatus.Done -> {
                        SnackbarData("删除成功")
                    }

                    is ActionStatus.Failed -> {
                        SnackbarData("删除失败", SnackbarStatus.Error)
                    }

                    else -> {
                        null
                    }
                }
            }
        )
    }
    var deleteBodyDialog by remember {
        mutableStateOf<BodyDataRecord?>(null)
    }
    val state = viewModel.bodyDetail.collectAsState().value
    BodyDetailPage(
        data = state,
        onCardLongClick = { deleteBodyDialog = it },
        onMonthChanged = { viewModel.month = it },
        onFieldFiltered = { viewModel.field = it },
    )

    deleteBodyDialog?.let {
        val dismiss by rememberUpdatedState(newValue = { deleteBodyDialog = null })
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {
                Text(
                    text = "删除",
                    modifier = Modifier
                        .clickable {
                            viewModel.deleteBodyDetail(it)
                            dismiss()
                        },
                    textAlign = TextAlign.Center
                )
            },
            dismissButton = {
                Text(
                    text = "取消",
                    modifier = Modifier
                        .clickable { dismiss() },
                    textAlign = TextAlign.Center
                )
            },
            title = {
                Text(text = "删除身体数据记录")
            },
            text = {
                val format = getLocalDateTimeFormatter(Locale.getDefault()).withZone(ZoneId.systemDefault())
                Text(
                    text = "你将要删除记录于${format.format(it.instant)}的身体数据记录。"
                )
            }
        )
    }
}

@Composable
fun BodyDetailPage(
    data: BodyDetailPageState,
    onCardLongClick: (BodyDataRecord) -> Unit,
    onMonthChanged: (YearMonth) -> Unit = {},
    onFieldFiltered: (BodyField) -> Unit = {},
) {
    val dates =
        data.list.personData.entries.sortedBy { it.key }.flatMap { map -> map.value.map { map.key to it } }

    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    val scaffoldProperty = LocalScaffoldProperty.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scaffoldProperty.scrollConnection),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = scaffoldProperty.padding,
    ) {
        item(key = "chart") {
            BodyChart(
                data = data,
                modifier = Modifier.fillMaxWidth(),
                onMonthChanged = onMonthChanged,
                onFieldFiltered = onFieldFiltered,
            )
        }
        itemsIndexed(dates, key = { _, item -> item.second.instant }) { index, it ->
            BodyDetailDaily(
                modifier = Modifier.padding(horizontal = 12.dp),
                day = it.first,
                data = it.second,
                format = formatter,
                onCardLongClick = onCardLongClick
            )
            if (index != dates.size - 1) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyChart(
    data: BodyDetailPageState,
    modifier: Modifier = Modifier,
    onMonthChanged: (YearMonth) -> Unit = {},
    onFieldFiltered: (BodyField) -> Unit = {},
) {
    Column(modifier = modifier) {
        CalendarHeader(
            month = data.month,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp),
            onMonthChanged = onMonthChanged
        )
        LineChart(
            line = data.chartLine,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 12.dp),
            displayMonth = data.month,
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BodyField.values().forEach {
                FilterChip(
                    selected = data.selectedField == it,
                    onClick = { onFieldFiltered(it) },
                    label = {
                        Text(text = it.label)
                    },
                    enabled = data.hasFieldData(it)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BodyDetailDaily(
    modifier: Modifier,
    day: LocalDate,
    data: BodyDataRecord,
    format: DateTimeFormatter,
    onCardLongClick: (BodyDataRecord) -> Unit,
) {
    Column(
        modifier = modifier
            .combinedClickable(onLongClick = { onCardLongClick(data) }) { }
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = format.format(day),
            modifier = Modifier.heightIn(min = 24.dp),
            style = MaterialTheme.typography.titleMedium
        )

        if (data.weight != 0f) {
            DataPair(name = "体重", content = "${data.weight} kg")
        }

        if (data.bustSize != 0f) {
            DataPair(name = "胸围", content = "${data.bustSize} cm")
        }

        if (data.waistSize != 0f) {
            DataPair(name = "腰围", content = "${data.waistSize} cm")
        }

        if (data.hipSize != 0f) {
            DataPair(name = "臀围", content = "${data.hipSize} cm")
        }

        if (data.bodyFat != 0f) {
            DataPair(name = "体脂率", content = "${data.bodyFat} %")
        }
    }
}

@Composable
private fun DataPair(name: String, content: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = name,
            modifier = Modifier.heightIn(min = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = content,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End
        )
    }
}