package site.xiaozk.dailyfitness.repository.model

import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import java.time.Instant
import java.time.LocalDate

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
data class DailyTrainAction(
    val instant: Instant,
    val action: TrainAction,
    val takenDuration: RecordedDuration?,
    val takenWeight: RecordedWeight?,
    val takenCount: Int,
    val note: String,
) {
    val displayText: List<String>
        get() = buildList {
            if (action.isWeightedAction && takenWeight != null) {
                add("$takenWeight")
            }
            if (action.isCountedAction && takenCount > 0) {
                add("x $takenCount")
            }
            if (action.isTimedAction && takenDuration != null) {
                add("$takenDuration")
            }
        }
}

@JvmInline
value class TrainingDayList(
    val trainedDate: Map<LocalDate, TrainingDayData> = emptyMap(),
)

data class TrainingDayData(
    val date: LocalDate,
    val trainedParts: List<DailyTrainingPartGroup> = emptyList(),
) {
    constructor(pair: Pair<LocalDate, List<DailyTrainingPartGroup>>) : this(pair.first, pair.second)
    val maxTrainedParts by lazy {
        trainedParts.maxByOrNull { it.actions.size }?.trainPart?.partName
    }
}

data class DailyTrainingPartGroup(
    val trainPart: TrainPart,
    val actions: List<DailyTrainingActionList>,
)

@JvmInline
value class DailyTrainingActionList(
    val map: Pair<TrainAction, List<DailyTrainAction>>,
) {
    val action: TrainAction
        get() = map.first

    val trainAction: List<DailyTrainAction>
        get() = map.second
}
