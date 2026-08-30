package site.xiaozk.dailyfitness.nav

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import site.xiaozk.dailyfitness.session.WorkoutSessionNavProvider
import javax.inject.Singleton

/**
 * App-side bindings on top of the [:session] module: supplies the navigation
 * capability the session notification requires.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppSessionModule {

    @Binds
    @Singleton
    abstract fun bindSessionNavProvider(
        impl: AppSessionNavProvider,
    ): WorkoutSessionNavProvider
}
