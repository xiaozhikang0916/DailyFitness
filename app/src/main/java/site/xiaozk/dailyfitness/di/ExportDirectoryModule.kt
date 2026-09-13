package site.xiaozk.dailyfitness.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import site.xiaozk.dailyfitness.export.AndroidExportDirectoryProvider
import site.xiaozk.dailyfitness.settings.ui.ExportDirectoryProvider
import javax.inject.Singleton

/**
 * Binds the app-provided export-directory picker used by the `:settings-ui`
 * export screen. This is the capability seam that keeps the platform file manager
 * out of the UI module.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExportDirectoryModule {
    @Binds
    @Singleton
    abstract fun bindExportDirectoryProvider(
        impl: AndroidExportDirectoryProvider,
    ): ExportDirectoryProvider
}
