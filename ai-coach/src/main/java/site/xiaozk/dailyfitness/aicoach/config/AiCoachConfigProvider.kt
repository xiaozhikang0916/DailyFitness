package site.xiaozk.dailyfitness.aicoach.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import site.xiaozk.dailyfitness.repository.IAiCoachConfigStore
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single reactive source of the AI Coach configuration.
 *
 * Engine and executor depend on this provider (instead of poking the store per
 * request); listeners observe [config] and rebuild caches on change.
 * The store keeps being the write path for the editing UI.
 */
@Singleton
class AiCoachConfigProvider @Inject constructor(
    configStore: IAiCoachConfigStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Latest persisted config; starts empty until the store emits its first value. */
    val config: StateFlow<AiCoachConfig> = configStore.observe()
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = AiCoachConfig())

    /** Convenience for one-shot reads (e.g. ConfigMissing checks). */
    val current: AiCoachConfig
        get() = config.value
}
