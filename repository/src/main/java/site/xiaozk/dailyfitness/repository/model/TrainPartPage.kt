package site.xiaozk.dailyfitness.repository.model

/**
 * @author: xiaozhikang
 * @create: 2023/3/19
 */
data class HomeTrainPartPage(
    val parts: List<TrainPartStaticPage>
) {
    val partCount = parts.size
    val actionCount = parts.sumOf { it.actionCount }
    val partWorkoutCount = parts.sumOf { it.partWorkoutCount }
    val workoutCount = parts.sumOf { it.workoutCount }
}

data class TrainPartStaticPage(
    val trainPart: TrainPart = TrainPart(),
    val actionCount: Int = 0,
    val partWorkoutCount: Int = 0,
    val workoutCount: Int = 0,
)

data class TrainActionStaticPage(
    val action: TrainAction = TrainAction(),
    val workoutCount: Int = 0,
    val workoutDays: Int = 0,
)