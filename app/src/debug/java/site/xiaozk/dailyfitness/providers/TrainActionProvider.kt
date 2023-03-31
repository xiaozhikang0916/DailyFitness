package site.xiaozk.dailyfitness.providers

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/24
 */
class TrainActionProvider : PreviewParameterProvider<TrainActionWithPart> {
    override val values: Sequence<TrainActionWithPart>
        get() {
            return sequenceOf(
                TrainActionWithPart(
                    part = TrainPart(1, "胸"), TrainAction(id = 1, partId = 1, actionName = "卧姿推胸", isTimedAction = false, isWeightedAction = true, isCountedAction = true)
                ),
                TrainActionWithPart(
                    part = TrainPart(2, "腿"), TrainAction(id = 2, partId = 2, actionName = "深蹲", isTimedAction = false, isWeightedAction = true, isCountedAction = true)
                ),
                TrainActionWithPart(
                    part = TrainPart(3, "背"), TrainAction(id = 3, partId = 3, actionName = "硬拉", isTimedAction = false, isWeightedAction = true, isCountedAction = true)
                ),
            )
        }
}

class TrainPartProvider : PreviewParameterProvider<TrainPartGroup> {
    override val values: Sequence<TrainPartGroup>
        get() = TrainActionProvider().values.groupBy { it.part }.map { TrainPartGroup(it.key, it.value) }.asSequence()
}

class TrainActionStaticPageProvider: PreviewParameterProvider<TrainActionStaticPage> {
    override val values: Sequence<TrainActionStaticPage>
        get() = TrainActionProvider().values.map {
            TrainActionStaticPage(it.action, DailyTrainActionProvider().values.toList())
        }
}

class TrainPartStaticPageProvider: PreviewParameterProvider<TrainPartStaticPage> {
    override val values: Sequence<TrainPartStaticPage>
        get() {
            val actionStatic: Map<TrainAction, List<TrainActionStaticPage>> = TrainActionStaticPageProvider().values.groupBy { it.action }
            return TrainPartProvider().values.map {
                TrainPartStaticPage(it.part, it.actions.flatMap { actionStatic[it.action].orEmpty() })
            }
        }
}

class HomeTrainPartPageProvider: PreviewParameterProvider<HomeTrainPartPage> {
    override val values: Sequence<HomeTrainPartPage>
        get() = sequenceOf(
            HomeTrainPartPage(
                TrainPartStaticPageProvider().values.toList()
            )
        )
}