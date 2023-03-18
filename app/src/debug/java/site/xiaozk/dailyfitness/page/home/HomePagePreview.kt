package site.xiaozk.dailyfitness.page.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import site.xiaozk.dailyfitness.page.training.TrainingHome
import site.xiaozk.dailyfitness.providers.DailyTrainPartProvider
import site.xiaozk.dailyfitness.repository.model.TrainingDayList
import site.xiaozk.dailyfitness.repository.model.TrainingDayData
import java.time.ZoneId

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/24
 */
@Preview
@Composable
fun PreviewHomePage(@PreviewParameter(HomePageDataProvider::class) data: TrainingDayList) {
    TrainingHome(data = data, onNav = {})
}

class HomePageDataProvider : PreviewParameterProvider<TrainingDayList> {
    override val values: Sequence<TrainingDayList>
        get() {
            val trainedDay = TrainedDayDateProvider().values
            return sequenceOf(
                TrainingDayList(
                    trainedDay.map {
                        it.date to it
                    }.toMap()
                )
            )
        }
}

class TrainedDayDateProvider : PreviewParameterProvider<TrainingDayData> {
    override val values: Sequence<TrainingDayData>
        get() {
            return DailyTrainPartProvider().values.map {
                it.actions.first().trainAction.first().instant.atZone(ZoneId.systemDefault()).toLocalDate() to it
            }.groupBy({ it.first }) { it.second }.entries.map { TrainingDayData(it.key, it.value.flatMap { it.actions }) }.asSequence()
        }

}