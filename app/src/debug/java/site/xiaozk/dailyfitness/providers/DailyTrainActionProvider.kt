package site.xiaozk.dailyfitness.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import site.xiaozk.dailyfitness.repository.model.DailyTrainAction
import site.xiaozk.dailyfitness.repository.model.DailyTrainingActionList
import site.xiaozk.dailyfitness.repository.model.DailyTrainingPartGroup
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import java.time.temporal.ChronoUnit
import java.util.Calendar

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/24
 */
class DailyTrainActionProvider : PreviewParameterProvider<DailyTrainAction> {
    override val values: Sequence<DailyTrainAction>
        get() {
            val actions = TrainActionProvider().values
            return actions.mapIndexed { index, it ->
                DailyTrainAction(
                    id = 0,
                    instant = Calendar.getInstance().toInstant().minus(index.toLong(), ChronoUnit.DAYS),
                    action = it,
                    takenDuration = RecordedDuration(0f),
                    takenWeight = RecordedWeight(15f),
                    takenCount = 20,
                    note = ""
                )
            }
        }
}

class DailyTrainPartProvider : PreviewParameterProvider<DailyTrainingPartGroup> {
    override val values: Sequence<DailyTrainingPartGroup>
        get() {
            val parts = TrainPartProvider().values
            val actions = DailyTrainActionProvider().values.groupBy { it.action.part }
            return parts.map {
                DailyTrainingPartGroup(
                    it.part,
                    actions[it.part]!!.groupBy { it.action }.entries.map { DailyTrainingActionList(it.toPair()) }
                )
            }
        }
}