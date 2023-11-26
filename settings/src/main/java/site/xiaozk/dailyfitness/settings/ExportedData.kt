package site.xiaozk.dailyfitness.settings

import kotlinx.serialization.Serializable
import site.xiaozk.dailyfitness.repository.model.BodyDataRecord
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.User

/**
 * @author: xiaozhikang
 * @create: 2023/11/25
 */
@Serializable
data class ExportedData(
    val userTrains: List<UserData>,
    val trainParts: List<TrainPartGroup>,
)

@Serializable
data class UserData(
    val user: User,
    val bodys: List<BodyDataRecord>,
    val workouts: List<DailyWorkoutAction>,
)