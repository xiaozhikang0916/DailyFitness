package site.xiaozk.dailyfitness.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

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
                    instant = Clock.System.now().minus(index.days),
                    action = it.action,
                    takenDuration = RecordedDuration(0f),
                    takenWeight = RecordedWeight(15f),
                    takenCount = 20,
                    note = ""
                )
            }
        }
}