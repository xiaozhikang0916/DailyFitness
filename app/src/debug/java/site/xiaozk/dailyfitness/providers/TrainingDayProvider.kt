package site.xiaozk.dailyfitness.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListActionPair
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListPartPair
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import java.time.ZoneId

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
class TrainingDayProvider : PreviewParameterProvider<DailyWorkout> {
    override val values: Sequence<DailyWorkout>
        get() {
            val parts = TrainPartProvider().values
            val action = DailyTrainActionProvider()
            val actions = action.values.groupBy { it.action.part }
            val localDate = action.values.first().instant.atZone(ZoneId.systemDefault()).toLocalDate()
            return parts.map {
                DailyWorkoutListPartPair(
                    it.part,
                    actions[it.part]!!.groupBy { it.action }.entries.map { DailyWorkoutListActionPair(it.toPair()) }
                )
            }.let {
                DailyWorkout(localDate, it.flatMap { it.actions }.toList())
            }.let {
                sequenceOf(it)
            }
        }
}