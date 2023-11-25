package site.xiaozk.dailyfitness.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.datetime.Instant
import site.xiaozk.dailyfitness.database.dao.BodyDao
import site.xiaozk.dailyfitness.database.dao.WorkoutDao
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.dao.UserDao
import site.xiaozk.dailyfitness.database.model.DBDailyBodyData
import site.xiaozk.dailyfitness.database.model.DBDailyWorkoutAction
import site.xiaozk.dailyfitness.database.model.DBTrainAction
import site.xiaozk.dailyfitness.database.model.DBTrainPart
import site.xiaozk.dailyfitness.database.model.DBUser

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/23
 */
@Database(
    entities = [
        DBDailyWorkoutAction::class,
        DBDailyBodyData::class,
        DBTrainAction::class,
        DBTrainPart::class,
        DBUser::class,
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2,
        )
    ]
)
@TypeConverters(InstantConverter::class)
abstract class AppDataBase : RoomDatabase() {
    abstract fun dailyDao(): WorkoutDao
    abstract fun trainDao(): TrainDao
    abstract fun userDao(): UserDao
    abstract fun bodyDao(): BodyDao
}

class InstantConverter {

    @TypeConverter
    fun instantToStamp(instant: Instant): Long {
        return instant.toEpochMilliseconds()
    }

    @TypeConverter
    fun stampToInstant(stamp: Long): Instant {
        return Instant.fromEpochMilliseconds(stamp)
    }
}