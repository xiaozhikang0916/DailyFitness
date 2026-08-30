package site.xiaozk.dailyfitness.session

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt wiring of the [:session] module. No global CoroutineScope is provided:
 * classes that need one own it themselves.
 *
 * [WorkoutSessionNavProvider] is a required binding supplied by the app module
 * (navigation capability lives in the app); a missing binding fails at build
 * time instead of silently degrading.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class WorkoutSessionModule {

    @Binds
    @Singleton
    abstract fun bindController(impl: WorkoutSessionControllerImpl): WorkoutSessionController

    @Binds
    @Singleton
    abstract fun bindSessionStore(impl: DataStoreSessionStore): SessionStore
}
