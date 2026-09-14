package site.xiaozk.dailyfitness.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListActionPair
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit


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

/**
 * One row of the ordered workout query: the recorded set, its action and the resolved
 * part name. The query orders rows by record time (oldest first), so this list order is
 * the record order.
 */
data class DBDailyWorkoutRecord(
    @Embedded val record: DBDailyWorkoutAction,
    @Embedded val action: DBTrainAction,
    @ColumnInfo(name = "partName") val partName: String,
)

/**
 * Folds an ordered (oldest-first) list of [DBDailyWorkoutRecord] into [DailyWorkout]s
 * while preserving that order: days oldest-first, action groups ordered by their earliest
 * set, and each action's sets oldest-first.
 */
fun List<DBDailyWorkoutRecord>.toDailyWorkoutList(
    zoneId: TimeZone = TimeZone.currentSystemDefault(),
): List<DailyWorkout> = groupBy { it.record.actionTime.toLocalDateTime(zoneId).date }
    .map { (date, rows) ->
        DailyWorkout(
            date = date,
            actions = rows.groupBy { it.action }.map { (action, actionRows) ->
                val part = DBTrainPart(id = action.partId, partName = actionRows.first().partName)
                DailyWorkoutListActionPair(
                    TrainActionWithPart(part.toRepoEntity(), action.toRepoAction()),
                    actionRows.map { (action to it.record).toDailyWorkoutAction() },
                )
            },
        )
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