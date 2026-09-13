package site.xiaozk.dailyfitness.aicoach.llm

import kotlinx.serialization.KSerializer
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage

/**
 * Thin seam between the engine (pure orchestration) and the koog LLM layer.
 * The engine passes pre-built prompt text, the previous conversation and a
 * serializer; implementations may be faked in unit tests without any network.
 *
 * Configuration is not passed per call: implementations read it from the shared
 * [site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider] and rebuild
 * their cached clients automatically when it changes.
 */
interface PlanExecutor {

    /**
     * Runs one structured request and returns the parsed reply.
     * [promptId] keeps koog prompt ids stable for tracing/caching;
     * [history] is sent as real multi-turn messages before [userText].
     */
    suspend fun <T> request(
        promptId: String,
        systemText: String,
        userText: String,
        history: List<CoachMessage> = emptyList(),
        serializer: KSerializer<T>,
    ): Result<T>

    /** Releases the cached HTTP client/executor and stops config observation. Idempotent. */
    fun close()
}
