package site.xiaozk.dailyfitness.page.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import site.xiaozk.dailyfitness.page.training.TrainingHome
import site.xiaozk.dailyfitness.providers.DailyTrainPartProvider
import site.xiaozk.dailyfitness.repository.model.WorkoutDayList
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import java.time.ZoneId

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/24
 */
@Preview
@Composable
fun PreviewHomePage(@PreviewParameter(HomePageDataProvider::class) data: WorkoutDayList) {
    TrainingHome(data = data, onNav = {})
}

class HomePageDataProvider : PreviewParameterProvider<WorkoutDayList> {
    override val values: Sequence<WorkoutDayList>
        get() {
            val trainedDay = TrainedDayDateProvider().values
            return sequenceOf(
                WorkoutDayList(
                    trainedDay.map {
                        it.date to it
                    }.toMap()
                )
            )
        }
}

class TrainedDayDateProvider : PreviewParameterProvider<DailyWorkout> {
    override val values: Sequence<DailyWorkout>
        get() {
            return DailyTrainPartProvider().values.map {
                it.actions.first().trainAction.first().instant.atZone(ZoneId.systemDefault()).toLocalDate() to it
            }.groupBy({ it.first }) { it.second }.entries.map { DailyWorkout(it.key, it.value.flatMap { it.actions }) }.asSequence()
        }

}