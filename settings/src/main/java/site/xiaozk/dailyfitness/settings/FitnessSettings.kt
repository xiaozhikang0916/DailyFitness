package site.xiaozk.dailyfitness.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.json.Json
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IPersonDailyRepository
import site.xiaozk.dailyfitness.repository.ISettingRepository
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author: xiaozhikang
 * @create: 2023/11/25
 */
@Singleton
class FitnessSettings
@Inject constructor(
    private val userRepo: IUserRepository,
    private val dailyWorkoutRepo: IDailyWorkoutRepository,
    private val personDataRepo: IPersonDailyRepository,
    private val trainRepo: ITrainActionRepository,
) : ISettingRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    override suspend fun exportAllDataTo(path: Path): Unit = withContext(Dispatchers.IO) {
        val allTranins = trainRepo.getAllTrainParts().first()
        val user = userRepo.getCurrentUser()
        val bodyData = personDataRepo.getAllPersonDailyDataFlow(user).toList()
        val workouts = dailyWorkoutRepo.getAllWorkoutDayList(user).first()

        val exportData = ExportedData(
            userTrains = listOf(
                UserData(
                    user = user,
                    bodys = bodyData.flatMap { it.personData.values }.flatten(),
                    workouts = workouts.trainedDate.flatMap { it.value.actions }
                        .flatMap { it.map.second },
                ),
            ),
            trainParts = allTranins
        )
        val outputJson = json.encodeToString(exportData)

        // kotlinx-io creates the file when needed; no java.io.File involved.
        // Failures propagate so the export UI can report them to the user.
        SystemFileSystem.sink(path).buffered().use { sink ->
            sink.writeString(outputJson)
        }
    }

    override suspend fun importAllDataFrom(path: Path): Unit = withContext(Dispatchers.IO) {
        val jsonStr = SystemFileSystem.source(path).buffered().use { source ->
            source.readString()
        }
        val data = json.decodeFromString<ExportedData>(jsonStr)
        val user = userRepo.getCurrentUser()

        data.trainParts.forEach {
            trainRepo.addTrainPart(it.part)
            it.actions.forEach { action ->
                trainRepo.addTrainAction(action.action)
            }
        }

        data.userTrains.forEach {
            it.bodys.forEach { body ->
                personDataRepo.addPersonDailyData(user, body)
            }
            it.workouts.forEach {
                dailyWorkoutRepo.addWorkoutAction(user, it)
            }
        }

    }
}
