package site.xiaozk.dailyfitness.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import site.xiaozk.dailyfitness.aicoach.engine.CoachLocaleProvider
import javax.inject.Singleton

/**
 * Host-side binding of the language the AI Coach answers in.
 *
 * The current app locale is read once when the singleton is created; a language
 * change therefore only takes effect after the process restarts. That is
 * intentional for now (the engine does not rebuild on configuration changes).
 */
@Module
@InstallIn(SingletonComponent::class)
object AiCoachLocaleModule {

    @Provides
    @Singleton
    fun provideCoachLocaleProvider(@ApplicationContext context: Context): CoachLocaleProvider {
        val languageTag = context.resources.configuration.locales[0].toLanguageTag()
        return CoachLocaleProvider { languageTag }
    }
}
