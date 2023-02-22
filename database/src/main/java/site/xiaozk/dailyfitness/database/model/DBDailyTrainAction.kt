package site.xiaozk.dailyfitness.database.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import site.xiaozk.dailyfitness.repository.model.DailyTrainAction
import site.xiaozk.dailyfitness.repository.model.DailyTrainingActionList
import site.xiaozk.dailyfitness.repository.model.DailyTrainingPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainingDayData
import site.xiaozk.dailyfitness.repository.model.TrainingDayList
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit
import java.time.Instant
import java.time.ZoneId


/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Entity(
    tableName = "daily_train_action",
    foreignKeys = [ForeignKey(entity = DBTrainAction::class, parentColumns = ["id"], childColumns = ["usingActionId"]),
        ForeignKey(entity = DBUser::class, parentColumns = ["uid"], childColumns = ["userId"])],
    indices = [Index("userId"), Index("usingActionId")]
)
data class DBDailyTrainAction(
    @PrimaryKey(autoGenerate = true) val actionId: Int = 0,
    val usingActionId: Int,
    val userId: Int,
    val actionTime: Instant,
    @Embedded val recordedDuration: DBRecordedDuration?,
    @Embedded val recordedWeight: DBRecordedWeight?,
    val takenCount: Int?,
    val note: String?,
)

fun DailyTrainAction.toDbEntity(userId: Int): DBDailyTrainAction {
    return DBDailyTrainAction(
        usingActionId = this.action.id,
        actionTime = this.instant,
        recordedDuration = DBRecordedDuration.fromRepo(this.takenDuration),
        recordedWeight = DBRecordedWeight.fromRepo(this.takenWeight),
        takenCount = this.takenCount,
        userId = userId,
        note = note.takeIf { it.isNotBlank() },
    )
}

data class DBRecordedWeight(
    val takenWeight: Float,
    val weightUnit: DBWeightUnit,
) {
    fun toRepoEntity(): RecordedWeight {
        return RecordedWeight(
            takenWeight,
            weightUnit.repoUnit
        )
    }

    companion object {
        fun fromRepo(weight: RecordedWeight?): DBRecordedWeight? {
            return if (weight != null ) {
                DBRecordedWeight(
                    takenWeight = weight.weight,
                    weightUnit = DBWeightUnit.fromRepo(weight.weightUnit)
                )
            } else {
                null
            }
        }
    }
}

enum class DBWeightUnit(val repoUnit: WeightUnit) {
    Kg(WeightUnit.Kg),
    Lbs(WeightUnit.Lbs),
    ;

    companion object {
        fun fromRepo(unit: WeightUnit): DBWeightUnit {
            return when (unit) {
                WeightUnit.Kg -> Kg
                WeightUnit.Lbs -> Lbs
            }
        }
    }
}

data class DBRecordedDuration(
    val takenDuration: Float,
    val timeUnit: DBTimeUnit,
) {
    fun toRepoEntity(): RecordedDuration {
        return RecordedDuration(
            takenDuration,
            timeUnit.repoUnit
        )
    }

    companion object {
        fun fromRepo(duration: RecordedDuration?): DBRecordedDuration? {
            return if (duration != null) {
                DBRecordedDuration(
                    takenDuration = duration.duration,
                    timeUnit = DBTimeUnit.fromRepo(duration.timeUnit)
                )
            } else {
                null
            }
        }
    }
}

enum class DBTimeUnit(val repoUnit: TimeUnit) {
    Sec(TimeUnit.Sec),
    Min(TimeUnit.Min),
    ;

    companion object {
        fun fromRepo(unit: TimeUnit): DBTimeUnit {
            return when (unit) {
                TimeUnit.Sec -> Sec
                TimeUnit.Min -> Min
            }
        }
    }
}

fun Map<DBTrainPart, Map<DBTrainAction, List<DBDailyTrainAction>>>.toTrainingDayList(): TrainingDayList {
    val zone = ZoneId.systemDefault()
    return this.flatMap {outMap ->
        outMap.value.flatMap { innerMap ->
            innerMap.value.map {
                outMap.key to (innerMap.key to it)
            }
        }
    }.map {
        DailyTrainAction(
            instant = it.second.second.actionTime,
            action = it.second.first.toRepoEntity(it.first),
            takenDuration = it.second.second.recordedDuration?.toRepoEntity(),
            takenWeight = it.second.second.recordedWeight?.toRepoEntity(),
            takenCount = it.second.second.takenCount ?: 0,
            note = it.second.second.note ?: ""
        )
    }.groupBy({ it.instant.atZone(zone).toLocalDate() }) {
        it.action to it
    }.entries.map { entry ->
        entry.key to entry.value.groupBy({ it.first }) { it.second }
    }.map {
        it.first to it.second.entries.map { entry -> DailyTrainingActionList(entry.toPair()) }
    }.map {
        it.first to it.second.groupBy { it.action.part }.map { DailyTrainingPartGroup(it.key, it.value) }
    }.associate {
        it.first to TrainingDayData(it)
    }.let { TrainingDayList(it) }

}