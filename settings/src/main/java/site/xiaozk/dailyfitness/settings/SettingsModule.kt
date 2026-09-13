package site.xiaozk.dailyfitness.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import site.xiaozk.dailyfitness.repository.ISettingRepository
import javax.inject.Singleton

/**
 * Binds the `:settings` implementation of the data import/export contract.
 *
 * The UI modules (`:settings-ui`, `:app`) only depend on [ISettingRepository];
 * they never reference [FitnessSettings] directly.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    @Singleton
    abstract fun bindSettingRepository(impl: FitnessSettings): ISettingRepository
}
