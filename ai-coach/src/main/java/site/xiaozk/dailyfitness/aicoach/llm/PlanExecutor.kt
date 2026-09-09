package site.xiaozk.dailyfitness.aicoach.llm

import kotlinx.serialization.KSerializer

/**
 * Thin seam between the engine (pure orchestration) and the koog LLM layer.
 * The engine passes pre-built prompt text and a serializer; implementations
 * may be faked in unit tests without any network.
 *
 * Configuration is not passed per call: implementations read it from the shared
 * [site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider] and rebuild
 * their cached clients automatically when it changes.
 */
interface PlanExecutor {

    /**
     * Runs one structured request and returns the parsed reply.
     * [promptId] keeps koog prompt ids stable for tracing/caching.
     */
    suspend fun <T> request(
        promptId: String,
        systemText: String,
        userText: String,
        serializer: KSerializer<T>,
    ): Result<T>

    /** Releases the cached HTTP client/executor and stops config observation. Idempotent. */
    fun close()
}
