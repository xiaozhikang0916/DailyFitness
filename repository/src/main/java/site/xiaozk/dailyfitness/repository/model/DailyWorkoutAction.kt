package site.xiaozk.dailyfitness.repository.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Serializable
data class DailyWorkoutAction(
    val id: Int = 0,
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

data class DailyWorkout(
    val date: LocalDate,
    val actions: List<DailyWorkoutListActionPair> = emptyList(),
) {
    constructor(pair: Pair<LocalDate, List<DailyWorkoutListActionPair>>) : this(pair.first, pair.second)
}

@JvmInline
value class DailyWorkoutMap(
    val trainedDate: HashMap<LocalDate, DailyWorkout> = HashMap(),
) {
    operator fun get(date: LocalDate): DailyWorkout? = trainedDate[date]
}

@JvmInline
value class DailyWorkoutListActionPair(
    val map: Pair<TrainActionWithPart, List<DailyWorkoutAction>>,
) {
    constructor(action: TrainActionWithPart, actions: List<DailyWorkoutAction>): this(Pair(action, actions))

    val action: TrainActionWithPart
        get() = map.first

    val trainAction: List<DailyWorkoutAction>
        get() = map.second
}
