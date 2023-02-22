package site.xiaozk.dailyfitness.database.inject

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import site.xiaozk.dailyfitness.database.AppDataBase
import site.xiaozk.dailyfitness.database.dao.DailyDao
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.dao.UserDao
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
    fun provideDailyDao(db: AppDataBase): DailyDao {
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
    @Singleton
    fun provideAppDataBase(@ApplicationContext context: Context):AppDataBase {
        return Room.databaseBuilder(
            context = context,
            klass = AppDataBase::class.java,
            "fitness.db"
        ).build()
    }
}