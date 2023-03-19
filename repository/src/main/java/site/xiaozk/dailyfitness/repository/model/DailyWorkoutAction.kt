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

@JvmInline
value class WorkoutDayList(
    val trainedDate: Map<LocalDate, DailyWorkout> = emptyMap(),
)

data class DailyWorkout(
    val date: LocalDate,
    val actions: List<DailyWorkoutListActionPair> = emptyList(),
) {
    constructor(pair: Pair<LocalDate, List<DailyWorkoutListActionPair>>) : this(pair.first, pair.second)
}

@JvmInline
value class DailyWorkoutListPartPair(
    val map: Pair<TrainPart, List<DailyWorkoutListActionPair>>,
) {

    constructor(trainPart: TrainPart, actions: List<DailyWorkoutListActionPair>): this(Pair(trainPart, actions))

    val trainPart: TrainPart
        get() = map.first

    val actions: List<DailyWorkoutListActionPair>
        get() = map.second
}

@JvmInline
value class DailyWorkoutListActionPair(
    val map: Pair<TrainAction, List<DailyWorkoutAction>>,
) {
    val action: TrainAction
        get() = map.first

    val trainAction: List<DailyWorkoutAction>
        get() = map.second
}
