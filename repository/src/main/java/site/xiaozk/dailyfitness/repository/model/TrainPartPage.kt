package site.xiaozk.dailyfitness.repository.model

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight

/**
 * @author: xiaozhikang
 * @create: 2023/3/19
 */
data class HomeTrainPartPage(
    val parts: List<TrainPartStaticPage> = emptyList()
) {
    val partCount = parts.size
    val actionCount = parts.sumOf { it.actionCount }
    val partWorkoutCount = parts.sumOf { it.partWorkoutCount }
    val workoutCount = parts.sumOf { it.workoutCount }
}

data class TrainPartStaticPage(
    val trainPart: TrainPart = TrainPart(),
    val actions: List<TrainActionStaticPage> = emptyList(),
) {
    val actionCount: Int = actions.size
    val partWorkoutCount: Int = actions.flatMap {
        it.workouts.map {
            it.instant.toLocalDateTime(
                TimeZone.currentSystemDefault()
            ).date
        }
    }.distinct().size
    val workoutCount: Int = actions.sumOf { it.workoutCount }
}

data class TrainActionStaticPage(
    val action: TrainAction = TrainAction(),
    val workouts: List<DailyWorkoutAction> = emptyList(),
) {
    val workoutCount: Int = workouts.size
    val maxWeight: DailyWorkoutAction? = if (action.isWeightedAction) workouts.maxByOrNull {
        it.takenWeight ?: RecordedWeight.Zero
    } else null
    val maxCount: DailyWorkoutAction? = if (action.isCountedAction) workouts.maxByOrNull {
        it.takenCount
    } else null
    val maxDuration: DailyWorkoutAction? = if (action.isTimedAction) workouts.maxByOrNull {
        it.takenDuration ?: RecordedDuration.Zero
    } else null

    val maxList: Map<String, Instant>
        get() = listOfNotNull(
            maxWeight?.let {
                it.takenWeight.toString() to it.instant
            },
            maxCount?.let {
                "x ${it.takenCount}" to it.instant
            },
            maxDuration?.let {
                it.takenDuration.toString() to it.instant
            }
        ).toMap()
}