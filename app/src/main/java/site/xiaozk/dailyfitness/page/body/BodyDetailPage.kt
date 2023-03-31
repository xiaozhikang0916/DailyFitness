package site.xiaozk.dailyfitness.page.body

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
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
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.HomepageScaffoldState
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/25
 */

@Composable
fun BodyDetailPage() {
    val viewModel: BodyViewModel = hiltViewModel()

    val appScaffoldViewModel: AppScaffoldViewModel = hiltViewModel()
    LaunchedEffect(key1 = Unit) {
        appScaffoldViewModel.scaffoldState.emit(
            HomepageScaffoldState(
                title = "身体数据",
            )
        )
    }
    var deleteBodyDialog by remember {
        mutableStateOf<BodyDataRecord?>(null)
    }
    BodyDetailPage(data = viewModel.bodyDetail.collectAsState(initial = BodyDataWithDate()).value) {
        deleteBodyDialog = it
    }

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
                val format = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())
                Text(
                    text = "你将要删除记录于${format.format(it.instant)}的身体数据记录。"
                )
            }
        )
    }
}

@Composable
fun BodyDetailPage(data: BodyDataWithDate, onCardLongClick: (BodyDataRecord) -> Unit) {
    val dates =
        data.personData.entries.sortedBy { it.key }.flatMap { map -> map.value.map { map.key to it } }

    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    val scaffoldProperty = LocalScaffoldProperty.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .nestedScroll(scaffoldProperty.scrollConnection),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = scaffoldProperty.padding,
    ) {
        itemsIndexed(dates) { index, it ->
            BodyDetailDaily(day = it.first, data = it.second, format = formatter, onCardLongClick = onCardLongClick)
            if (index != dates.size - 1) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BodyDetailDaily(
    day: LocalDate,
    data: BodyDataRecord,
    format: DateTimeFormatter,
    onCardLongClick: (BodyDataRecord) -> Unit,
) {
    Column(
        modifier = Modifier
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