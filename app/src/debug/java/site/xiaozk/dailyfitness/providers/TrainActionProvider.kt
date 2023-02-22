package site.xiaozk.dailyfitness.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/24
 */
class TrainActionProvider : PreviewParameterProvider<TrainAction> {
    override val values: Sequence<TrainAction>
        get() {
            return sequenceOf(
                TrainAction(
                    id = 1, actionName = "卧姿推胸", part = TrainPart(1, "胸"), isTimedAction = false, isWeightedAction = true, isCountedAction = true
                ),
                TrainAction(
                    id = 2, actionName = "深蹲", part = TrainPart(2, "腿"), isTimedAction = false, isWeightedAction = true, isCountedAction = true
                ),
                TrainAction(
                    id = 3, actionName = "硬拉", part = TrainPart(3, "背"), isTimedAction = false, isWeightedAction = true, isCountedAction = true
                ),
            )
        }
}

class TrainPartProvider : PreviewParameterProvider<TrainPartGroup> {
    override val values: Sequence<TrainPartGroup>
        get() = TrainActionProvider().values.groupBy { it.part }.map { TrainPartGroup(it.key, it.value) }.asSequence()
}