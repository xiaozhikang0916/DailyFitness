package site.xiaozk.dailyfitness.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */

@Entity(tableName = "train_part")
data class DBTrainPart(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "part_name") val partName: String,
) {
    fun toRepoEntity(): TrainPart {
        return TrainPart(
            id = this.id,
            partName = this.partName,
        )
    }
}

fun TrainPart.toDbEntity() : DBTrainPart {
    return DBTrainPart(id = this.id, partName = this.partName)
}

fun Map.Entry<DBTrainPart, List<DBTrainAction>?>.toRepoEntity(): TrainPartGroup {
    return TrainPartGroup(
        part = key.toRepoEntity(),
        actions = value?.map { it.toRepoEntity(key) } ?: emptyList()
    )
}