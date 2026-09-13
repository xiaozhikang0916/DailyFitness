package site.xiaozk.dailyfitness.settings.ui.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Clock
import javax.inject.Singleton

/**
 * Provides the time source used to build export file names.
 *
 * Kept behind Hilt so tests can substitute a fixed [Clock] and assert the exact
 * file name the export screen produces.
 */
@Module
@InstallIn(SingletonComponent::class)
object SettingsUiModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
}
