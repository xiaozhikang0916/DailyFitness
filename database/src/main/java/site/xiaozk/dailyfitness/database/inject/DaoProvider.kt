package site.xiaozk.dailyfitness.database.inject

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import site.xiaozk.dailyfitness.database.AppDataBase
import site.xiaozk.dailyfitness.database.dao.BodyDao
import site.xiaozk.dailyfitness.database.dao.WorkoutDao
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.dao.UserDao
import java.util.Optional
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */

@InstallIn(SingletonComponent::class)
@Module
class DaoProvider {

    @Provides
    fun provideDailyDao(db: AppDataBase): WorkoutDao {
        return db.dailyDao()
    }
    @Provides
    fun provideTrainDao(db: AppDataBase): TrainDao {
        return db.trainDao()
    }
    @Provides
    fun provideUserDao(db: AppDataBase): UserDao {
        return db.userDao()
    }
    @Provides
    fun provideBodyDao(db: AppDataBase): BodyDao {
        return db.bodyDao()
    }

    @Provides
    @Singleton
    fun provideAppDataBaseBuilder(@ApplicationContext context: Context):RoomDatabase.Builder<AppDataBase> {
        return Room.databaseBuilder(
            context = context,
            klass = AppDataBase::class.java,
            "fitness.db"
        )
    }

    @Provides
    @Singleton
    fun provideAppDatabase(builder: RoomDatabase.Builder<AppDataBase>, @DebugDatabaseBuilder debugBuilder: Optional<RoomDatabase.Builder<AppDataBase>>): AppDataBase {
        return if (debugBuilder.isPresent) {
            debugBuilder.get().build()
        } else {
            builder.build()
        }
    }


}

@Qualifier annotation class DebugDatabaseBuilder

@InstallIn(SingletonComponent::class)
@Module
interface DebugDatabaseModule {
    @BindsOptionalOf
    @DebugDatabaseBuilder
    fun getDebugDatabaseBuilder(): RoomDatabase.Builder<AppDataBase>
}