@file:JvmName("TrainPartPageKt")

package site.xiaozk.dailyfitness.page.action

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage

/**
 * @author: xiaozhikang
 * @create: 2023/3/22
 */
@Composable
fun TrainPartPage(
    trainPartStaticPage: TrainPartStaticPage,
    onTrainActionClick: (TrainActionStaticPage) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TrainPartCard(
                trainPartStaticPage = trainPartStaticPage,
                modifier = Modifier.fillMaxWidth(),
                isHead = true,
            )
        }
        items(trainPartStaticPage.actions) {
            TrainActionCard(actionPage = it, modifier = Modifier
                .fillMaxWidth()
                .clickable { onTrainActionClick(it) })
        }
    }
}