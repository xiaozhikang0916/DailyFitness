package site.xiaozk.dailyfitness.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import site.xiaozk.dailyfitness.repository.model.DailyTrainingActionList
import site.xiaozk.dailyfitness.repository.model.DailyTrainingPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainingDayData
import java.time.ZoneId

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
class TrainingDayProvider : PreviewParameterProvider<TrainingDayData> {
    override val values: Sequence<TrainingDayData>
        get() {
            val parts = TrainPartProvider().values
            val action = DailyTrainActionProvider()
            val actions = action.values.groupBy { it.action.part }
            val localDate = action.values.first().instant.atZone(ZoneId.systemDefault()).toLocalDate()
            return parts.map {
                DailyTrainingPartGroup(
                    it.part,
                    actions[it.part]!!.groupBy { it.action }.entries.map { DailyTrainingActionList(it.toPair()) }
                )
            }.let {
                TrainingDayData(localDate, it.toList())
            }.let {
                sequenceOf(it)
            }
        }
}