package site.xiaozk.dailyfitness.aicoach.ui.a2ui

import androidx.a2ui.compose.ui.toJsonSchemaString
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import site.xiaozk.dailyfitness.aicoach.a2ui.A2uiCapabilityProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-side [A2uiCapabilityProvider]: exposes [DailyFitnessA2uiCatalog] to the
 * domain layer (prompt building) without pulling Compose into `:ai-coach`.
 */
@Singleton
class DailyFitnessA2uiCapabilityProvider @Inject constructor() : A2uiCapabilityProvider {

    override val catalogId: String = DailyFitnessA2uiCatalog.ID

    override fun inlineCatalogJson(): String = DailyFitnessA2uiCatalog.catalog.toJsonSchemaString()
}

/** Hilt wiring of the A2UI capabilities exposed by this module. */
@Module
@InstallIn(SingletonComponent::class)
abstract class A2uiCapabilityModule {

    @Binds
    @Singleton
    abstract fun bindA2uiCapabilityProvider(
        impl: DailyFitnessA2uiCapabilityProvider,
    ): A2uiCapabilityProvider
}
