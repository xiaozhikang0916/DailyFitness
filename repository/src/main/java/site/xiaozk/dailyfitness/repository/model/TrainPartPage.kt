package site.xiaozk.dailyfitness.repository.model

import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import java.time.Instant
import java.time.ZoneId

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
    val partWorkoutCount: Int = actions.flatMap { it.workouts.map { it.instant.atZone(ZoneId.systemDefault()).toLocalDate() } }.distinct().size
    val workoutCount: Int = actions.sumOf { it.workoutCount }
}

data class TrainActionStaticPage(
    val action: TrainAction = TrainAction(),
    val workouts: List<DailyWorkoutAction> = emptyList(),
) {
    val workoutCount: Int = workouts.size
    val workoutDays: Int = workouts.map { it.instant.atZone(ZoneId.systemDefault()).toLocalDate() }.distinct().size
}