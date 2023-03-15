package site.xiaozk.dailyfitness.page.body

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.AddDailyBodyDetailNavItem
import site.xiaozk.dailyfitness.nav.AppHomeRootNav
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.updateAppScaffoldState
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.BodyDataWithDate
import site.xiaozk.dailyfitness.repository.model.average
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
    val localNav = LocalNavController.current
    localNav?.navController?.let {
        AppHomeRootNav.AppHomePage.BodyDetailNavItem.updateAppScaffoldState(
            navController = it
        ) {
            fabRoute = AddDailyBodyDetailNavItem.route
            showBottomNavBar = true
        }
    }
    var deleteBodyDialog by remember {
        mutableStateOf<BodyDataRecord?>(null)
    }
    BodyDetailPage(data = viewModel.bodyDetail.collectAsState(initial = BodyDataWithDate()).value) {
        deleteBodyDialog = it
    }

    if (deleteBodyDialog != null) {
        val dismiss by rememberUpdatedState(newValue = { deleteBodyDialog = null })
        AlertDialog(
            onDismissRequest = dismiss,
            confirmButton = {

                Text(
                    text = "删除",
                    modifier = Modifier
                        .clickable {
                            deleteBodyDialog?.let {
                                viewModel.deleteBodyDetail(it)
                            }
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
                Text(text = "确认删除？")
            }
        )
    }
}

@Composable
fun BodyDetailPage(data: BodyDataWithDate, onCardLongClick: (BodyDataRecord) -> Unit) {
    val dates =
        data.personData.entries.sortedBy { it.key }

    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(dates) {
                BodyDetailDaily(day = it.key, data = it.value.average(), format = formatter, onCardLongClick = onCardLongClick)
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
    Card(
        modifier = Modifier
            .padding(all = 6.dp)
            .combinedClickable(onLongClick = { onCardLongClick(data) }) { }
            .fillMaxWidth(),
    ) {
        Text(
            text = format.format(day),
            fontSize = 14.sp,
        )

        if (data.weight != 0f) {
            Text(text = "体重 ${data.weight} kg")
        }

        if (data.bustSize != 0f) {
            Text(text = "胸围 ${data.bustSize} cm")
        }

        if (data.waistSize != 0f) {
            Text(text = "腰围 ${data.waistSize} cm")
        }

        if (data.hipSize != 0f) {
            Text(text = "臀围 ${data.hipSize} cm")
        }
    }
}