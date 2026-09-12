package site.xiaozk.dailyfitness.aicoach.llm

import ai.koog.http.client.KoogHttpClient
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.deepseek.DeepSeekClientSettings
import ai.koog.prompt.executor.clients.deepseek.DeepSeekLLMClient
import ai.koog.prompt.executor.clients.deepseek.DeepSeekModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.StructureFixingParser
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.KSerializer
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import site.xiaozk.dailyfitness.repository.model.AiCoachModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A cached LLM session (client + executor) that can be closed.
 *
 * KoogPlanExecutor only caches [LlmSession] instances and calls
 * [LlmSession.structuredRequest]; faking this type lets unit tests observe cache
 * creation/teardown without any real client or network.
 */
interface LlmSession : AutoCloseable {

    suspend fun <T> structuredRequest(
        config: AiCoachConfig,
        promptId: String,
        systemText: String,
        userText: String,
        history: List<CoachMessage> = emptyList(),
        serializer: KSerializer<T>,
    ): Result<T>

    override fun close() {}
}

/** Creates sessions; the cache key (apiKey + baseUrl) is managed by the caller. */
fun interface LlmSessionFactory {
    fun create(config: AiCoachConfig): LlmSession
}

/** DeepSeek-backed [LlmSession]. */
internal class KoogLlmSession(
    private val executor: MultiLLMPromptExecutor,
) : LlmSession {

    override suspend fun <T> structuredRequest(
        config: AiCoachConfig,
        promptId: String,
        systemText: String,
        userText: String,
        history: List<CoachMessage>,
        serializer: KSerializer<T>,
    ): Result<T> {
        val model = config.model.toKoogModel()
        val prompt = buildAiCoachPrompt(promptId, systemText, userText, history)
        return runCatching {
            executor.executeStructured(
                prompt = prompt,
                model = model,
                serializer = serializer,
                fixingParser = StructureFixingParser(model = model, retries = 1),
            ).getOrThrow().data
        }
    }

    override fun close() {
        executor.close()
    }
}

/**
 * Real [LlmSessionFactory] building a DeepSeek client with the host-injected
 * [KoogHttpClient.Factory] (see the :app AiCoachHttpModule binding).
 */
@Singleton
class RealLlmSessionFactory @Inject constructor(
    private val httpClientFactory: KoogHttpClient.Factory,
) : LlmSessionFactory {

    override fun create(config: AiCoachConfig): LlmSession {
        val settings = config.baseUrl?.let { DeepSeekClientSettings(baseUrl = it) }
            ?: DeepSeekClientSettings()
        val client = DeepSeekLLMClient(
            apiKey = config.apiKey,
            settings = settings,
            httpClientFactory = httpClientFactory,
        )
        return KoogLlmSession(MultiLLMPromptExecutor(LLMProvider.DeepSeek to client))
    }
}

/**
 * Builds the multi-turn koog prompt: system rules, then the previous conversation
 * as alternating user/assistant messages (newest last), then the fresh user turn.
 *
 * Extracted (internal) so unit tests can assert the exact message roles/texts
 * without any network.
 */
internal fun buildAiCoachPrompt(
    promptId: String,
    systemText: String,
    userText: String,
    history: List<CoachMessage>,
) = prompt(promptId) {
    system(systemText)
    history.forEach { message ->
        val text = message.content.toPromptText()
        if (message.fromUser) user(text) else assistant(text)
    }
    user(userText)
}

internal fun AiCoachModel.toKoogModel(): LLModel = when (this) {
    AiCoachModel.DeepSeekV4Flash -> DeepSeekModels.DeepSeekV4Flash
    AiCoachModel.DeepSeekV4Pro -> DeepSeekModels.DeepSeekV4Pro
}
