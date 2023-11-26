package site.xiaozk.dailyfitness.settings

import android.util.Log
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.IPersonDailyRepository
import site.xiaozk.dailyfitness.repository.ISettingRepository
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import java.io.File
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @create: 2023/11/25
 */
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

    override suspend fun exportAllDataTo(file: File): Unit = withContext(Dispatchers.IO) {
        try {
            val minDay =
                LocalDate.fromEpochDays(java.time.LocalDate.MIN.toEpochDay().toInt())
            val maxDay =
                LocalDate.fromEpochDays(java.time.LocalDate.MAX.toEpochDay().toInt())
            val allTranins = trainRepo.getAllTrainParts().first()
            val user = userRepo.getCurrentUser()
            val bodyData = personDataRepo.getPersonDailyDataFlow(
                user,
                minDay,
                maxDay,
            ).toList()
            val workouts = dailyWorkoutRepo.getWorkoutDayList(
                user,
                minDay,
                maxDay
            ).first()

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
            if (file.exists().not()) {
                file.createNewFile()
            }

            file.outputStream().use {
                it.write(
                    outputJson.encodeToByteArray()
                )
            }
        } catch (e: Exception) {
            Log.e("FitnessSettings", "fail to export data", e)
        }
    }

    override suspend fun importAllDataFrom(file: File): Unit = withContext(Dispatchers.IO) {
        val jsonStr = file.readBytes().decodeToString()
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

    private suspend fun clearCurrentData() {
        val minDay =
            LocalDate.fromEpochDays(java.time.LocalDate.MIN.toEpochDay().toInt())
        val maxDay =
            LocalDate.fromEpochDays(java.time.LocalDate.MAX.toEpochDay().toInt())
        val user = userRepo.getCurrentUser()
        dailyWorkoutRepo.getWorkoutDayList(
            user,
            minDay,
            maxDay
        ).first().trainedDate.flatMap { it.value.actions }.flatMap {
            it.trainAction
        }.forEach {
            dailyWorkoutRepo.deleteWorkoutAction(user, it)
        }

        trainRepo.getAllTrainParts().first().forEach { group ->
            group.actions.forEach { action ->
                trainRepo.removeTrainAction(action.action)
            }
            trainRepo.removeTrainPart(group.part)
        }

        personDataRepo.getPersonDailyDataFlow(user, minDay, maxDay).first().personData.flatMap {
            it.value
        }.forEach {
            personDataRepo.removePersonDailyData(it)
        }
    }
}