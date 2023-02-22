package site.xiaozk.dailyfitness.database.inject

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import site.xiaozk.dailyfitness.database.dao.DailyDao
import site.xiaozk.dailyfitness.database.dao.TrainDao
import site.xiaozk.dailyfitness.database.dao.UserDao
import site.xiaozk.dailyfitness.database.repo.PersonDailyRepository
import site.xiaozk.dailyfitness.database.repo.TrainActionRepository
import site.xiaozk.dailyfitness.database.repo.TrainingDayRepository
import site.xiaozk.dailyfitness.database.repo.UserRepository
import site.xiaozk.dailyfitness.repository.IPersonDailyRepository
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.ITrainingDayRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import javax.inject.Singleton

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/1
 */

@InstallIn(SingletonComponent::class)
@Module
class RepoProvider {
    @Provides
    fun providePersonDailyRepo(dailyDao: DailyDao): IPersonDailyRepository {
        return PersonDailyRepository(dailyDao)
    }

    @Provides
    fun provideTrainActionRepo(trainDao: TrainDao): ITrainActionRepository {
        return TrainActionRepository(trainDao)
    }

    @Provides
    fun provideTrainingDayRepo(dailyDao: DailyDao, trainDao: TrainDao): ITrainingDayRepository {
        return TrainingDayRepository(dailyDao, trainDao)
    }

    @Provides
    @Singleton
    fun provideUserRepo(userDao: UserDao): IUserRepository {
        return UserRepository(userDao)
    }
}