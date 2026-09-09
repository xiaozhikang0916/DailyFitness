package site.xiaozk.dailyfitness.aicoach.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import site.xiaozk.dailyfitness.aicoach.config.DataStoreAiCoachConfigStore
import site.xiaozk.dailyfitness.aicoach.engine.AiCoachEngine
import site.xiaozk.dailyfitness.aicoach.engine.IAiCoach
import site.xiaozk.dailyfitness.aicoach.llm.KoogPlanExecutor
import site.xiaozk.dailyfitness.aicoach.llm.LlmSessionFactory
import site.xiaozk.dailyfitness.aicoach.llm.PlanExecutor
import site.xiaozk.dailyfitness.aicoach.llm.RealLlmSessionFactory
import site.xiaozk.dailyfitness.repository.IAiCoachConfigStore
import javax.inject.Singleton

/**
 * Hilt wiring of the [:ai-coach] module.
 *
 * Repository interface bindings (IDailyWorkoutRepository / ITrainActionRepository /
 * IUserRepository) are provided by the :database module and resolved in the app graph.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiCoachModule {

    @Binds
    @Singleton
    abstract fun bindConfigStore(impl: DataStoreAiCoachConfigStore): IAiCoachConfigStore

    @Binds
    @Singleton
    abstract fun bindLlmSessionFactory(impl: RealLlmSessionFactory): LlmSessionFactory

    @Binds
    @Singleton
    abstract fun bindPlanExecutor(impl: KoogPlanExecutor): PlanExecutor

    @Binds
    @Singleton
    abstract fun bindAiCoach(impl: AiCoachEngine): IAiCoach
}
