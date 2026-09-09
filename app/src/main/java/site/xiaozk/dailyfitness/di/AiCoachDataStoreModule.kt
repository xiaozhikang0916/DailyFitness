package site.xiaozk.dailyfitness.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Host-side wiring of the AI Coach config DataStore.
 *
 * The only place that may touch a Context to obtain the [DataStore]; the
 * :ai-coach config store stays Android-free and receives this instance via DI.
 */
private val Context.aiCoachConfigDataStore by preferencesDataStore(name = "ai_coach_config")

@Module
@InstallIn(SingletonComponent::class)
object AiCoachDataStoreModule {

    @Provides
    @Singleton
    fun provideAiCoachConfigDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.aiCoachConfigDataStore
}
