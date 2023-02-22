package site.xiaozk.dailyfitness.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import site.xiaozk.dailyfitness.repository.model.TrainAction

@Entity(
    tableName = "train_action",
    foreignKeys = [ForeignKey(
        entity = DBTrainPart::class,
        parentColumns = ["id"],
        childColumns = ["partId"]
    )],
    indices = [Index("partId")]
)
data class DBTrainAction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "action_name") val actionName: String,
    val partId: Int,
    val isTimedAction: Boolean,
    val isWeightedAction: Boolean,
    val isCountedAction: Boolean,
) {
    fun toRepoEntity(part: DBTrainPart): TrainAction {

        return TrainAction(
            id = this.id,
            actionName = this.actionName,
            part = part.toRepoEntity(),
            isTimedAction = this.isTimedAction,
            isWeightedAction = this.isWeightedAction,
            isCountedAction = this.isCountedAction
        )
    }
}
