package site.xiaozk.dailyfitness.aicoach.llm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * koog-backed [PlanExecutor] (DeepSeek).
 *
 * Config is injected via [AiCoachConfigProvider]; sessions are created through the
 * injectable [LlmSessionFactory] (the host decides the HTTP transport). The cache
 * is keyed by apiKey+baseUrl and rebuilt automatically when the config changes
 * (observed in a background scope) or lazily on request; clearing the apiKey
 * destroys the cached session.
 */
@Singleton
class KoogPlanExecutor @Inject constructor(
    private val configProvider: AiCoachConfigProvider,
    private val sessionFactory: LlmSessionFactory,
) : PlanExecutor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private data class Cached(
        val apiKey: String,
        val baseUrl: String?,
        val session: LlmSession,
    )

    private val lock = Mutex()
    private var cached: Cached? = null

    init {
        // Cleanup: when the scope is cancelled (close()), destroy the cached session
        // in a NonCancellable context so teardown always completes.
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { destroyCached() }
            }
        }
        // Observe config changes: rebuild the cached session (or destroy it when the
        // key is cleared).
        scope.launch {
            configProvider.config
                .drop(1) // initial value is applied lazily by the first request
                .collect { config ->
                    if (config.apiKey.isBlank()) destroyCached() else rebuildIfNeeded(config)
                }
        }
    }

    override suspend fun <T> request(
        promptId: String,
        systemText: String,
        userText: String,
        history: List<CoachMessage>,
        serializer: KSerializer<T>,
    ): Result<T> {
        val config = configProvider.current
        if (config.apiKey.isBlank()) {
            return Result.failure(IllegalStateException("AI 未配置 API Key"))
        }
        val cached = rebuildIfNeeded(config)
        return cached.session.structuredRequest(config, promptId, systemText, userText, history, serializer)
    }

    override fun close() {
        // Cancellation triggers the awaitCancellation cleanup coroutine, which destroys
        // the cached session inside withContext(NonCancellable). No blocking wait here.
        scope.cancel()
    }

    private suspend fun destroyCached() {
        lock.withLock {
            cached?.session?.close()
            cached = null
        }
    }

    private suspend fun rebuildIfNeeded(config: AiCoachConfig): Cached = lock.withLock {
        val existing = cached
        if (existing != null && existing.apiKey == config.apiKey && existing.baseUrl == config.baseUrl) {
            return@withLock existing
        }
        cached?.session?.close()
        val session = sessionFactory.create(config)
        return@withLock Cached(config.apiKey, config.baseUrl, session).also { cached = it }
    }
}
