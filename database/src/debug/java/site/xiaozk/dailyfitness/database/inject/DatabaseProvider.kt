package site.xiaozk.dailyfitness.database.inject

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import site.xiaozk.dailyfitness.database.AppDataBase
import javax.inject.Singleton

/**
 * @author: xiaozhikang
 * @create: 2023/3/7
 */

@InstallIn(SingletonComponent::class)
@Module
class DatabaseProvider {
    @Provides
    @Singleton
    @DebugDatabaseBuilder
    fun provideDatabase(@ApplicationContext context: Context, builder: RoomDatabase.Builder<AppDataBase>): RoomDatabase.Builder<AppDataBase> {
        return builder.setQueryCallback(queryCallback = object : RoomDatabase.QueryCallback {
            override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                Log.d("Database", "Querying $sqlQuery, args $bindArgs")
            }
        }, executor = ContextCompat.getMainExecutor(context))
    }
}