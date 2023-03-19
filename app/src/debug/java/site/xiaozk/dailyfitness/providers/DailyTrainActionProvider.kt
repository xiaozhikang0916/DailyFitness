package site.xiaozk.dailyfitness.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListActionPair
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListPartPair
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import java.time.temporal.ChronoUnit
import java.util.Calendar

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/24
 */
class DailyTrainActionProvider : PreviewParameterProvider<DailyWorkoutAction> {
    override val values: Sequence<DailyWorkoutAction>
        get() {
            val actions = TrainActionProvider().values
            return actions.mapIndexed { index, it ->
                DailyWorkoutAction(
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

class DailyTrainPartProvider : PreviewParameterProvider<DailyWorkoutListPartPair> {
    override val values: Sequence<DailyWorkoutListPartPair>
        get() {
            val parts = TrainPartProvider().values
            val actions = DailyTrainActionProvider().values.groupBy { it.action.part }
            return parts.map {
                DailyWorkoutListPartPair(
                    it.part,
                    actions[it.part]!!.groupBy { it.action }.entries.map { DailyWorkoutListActionPair(it.toPair()) }
                )
            }
        }
}