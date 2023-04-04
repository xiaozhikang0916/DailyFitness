package site.xiaozk.dailyfitness.database.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListActionPair
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutMap
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutSummary
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import site.xiaozk.dailyfitness.repository.model.WorkoutDaySummaryMap
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit
import java.time.Instant
import java.time.ZoneId
import java.util.TreeMap


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
data class DBDailyWorkoutAction(
    @PrimaryKey(autoGenerate = true) val actionId: Int = 0,
    val usingActionId: Int,
    val userId: Int,
    val actionTime: Instant,
    @Embedded val recordedDuration: DBRecordedDuration?,
    @Embedded val recordedWeight: DBRecordedWeight?,
    val takenCount: Int?,
    val note: String?,
)

fun DailyWorkoutAction.toDbEntity(userId: Int): DBDailyWorkoutAction {
    return DBDailyWorkoutAction(
        actionId = this.id,
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
            return if (weight != null) {
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

fun Map<DBTrainPart, Map<DBTrainAction, List<DBDailyWorkoutAction>>>.toWorkoutDailyMap(): DailyWorkoutMap {
    val zone = ZoneId.systemDefault()
    return this.flatMap { outMap ->
        outMap.value.flatMap { innerMap ->
            innerMap.value.map {
                outMap.key to (innerMap.key to it)
            }
        }
    }.map {
        it.second.toDailyWorkoutAction() to it.first.toRepoEntity()
    }.groupBy({ it.first.instant.atZone(zone).toLocalDate() }) {
        it.first.action to it
    }.entries.map { entry ->
        entry.key to entry.value.groupBy({ it.first to it.second.second }) { it.second.first }
    }.map {
        it.first to it.second.entries.map { entry -> DailyWorkoutListActionPair(TrainActionWithPart(entry.key.second, entry.key.first), entry.value) }
    }.associate {
        it.first to DailyWorkout(it)
    }.let {
        DailyWorkoutMap(HashMap(it))
    }
}

fun Map<DBTrainPart, Map<DBTrainAction, List<DBDailyWorkoutAction>>>.toWorkoutSummary(): WorkoutDaySummaryMap {
    return WorkoutDaySummaryMap(TreeMap(this.toWorkoutDailyMap().trainedDate.mapValues { kv -> DailyWorkoutSummary(kv.value) }))
}

fun Pair<DBTrainAction, DBDailyWorkoutAction>.toDailyWorkoutAction(): DailyWorkoutAction {
    return DailyWorkoutAction(
        id = second.actionId,
        instant = second.actionTime,
        action = first.toRepoAction(),
        takenDuration = second.recordedDuration?.toRepoEntity(),
        takenWeight = second.recordedWeight?.toRepoEntity(),
        takenCount = second.takenCount ?: 0,
        note = second.note ?: ""
    )
}

fun Pair<DBTrainAction, List<DBDailyWorkoutAction>>.toTrainActionStatics(): TrainActionStaticPage {
    return TrainActionStaticPage(
        action = first.toRepoAction(),
        workouts = second.map { first to it }.map { it.toDailyWorkoutAction() }
    )
}
fun Pair<DBTrainPart, Map<DBTrainAction, List<DBDailyWorkoutAction>>>.toTrainPartStaticPage(): TrainPartStaticPage {
    return TrainPartStaticPage(
        trainPart = first.toRepoEntity(),
        actions = second.map { it.toPair().toTrainActionStatics() }
    )
}
fun Map<DBTrainPart, Map<DBTrainAction, List<DBDailyWorkoutAction>>>.toHomeTrainPartPage(): HomeTrainPartPage {
    return HomeTrainPartPage(
        parts = this.map { it.toPair().toTrainPartStaticPage() }
    )
}