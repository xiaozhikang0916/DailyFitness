package site.xiaozk.dailyfitness.aicoach.llm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import org.junit.Assert.assertEquals
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.FakeConfigStore
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.awaitUntil
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import site.xiaozk.dailyfitness.repository.model.AiCoachModel

/**
 * Cache lifecycle of [KoogPlanExecutor]: sessions are created/destroyed on config
 * changes. Uses a fake [LlmSessionFactory], so no client or network is involved.
 */
class KoogPlanExecutorTest {

    private class FakeSession(
        private val apiKey: String,
        private val onClose: (String) -> Unit,
    ) : LlmSession {
        override suspend fun <T> structuredRequest(
            config: AiCoachConfig,
            promptId: String,
            systemText: String,
            userText: String,
            serializer: KSerializer<T>,
        ): Result<T> = Result.failure(IllegalStateException("not used in this test"))

        override fun close() = onClose(apiKey)
    }

    private class FakeSessionFactory : LlmSessionFactory {
        val createdCount = MutableStateFlow(0)
        val createdKeys = MutableStateFlow<List<String>>(emptyList())
        val closedKeys = MutableStateFlow<List<String>>(emptyList())

        override fun create(config: AiCoachConfig): LlmSession {
            createdCount.update { it + 1 }
            createdKeys.update { it + config.apiKey }
            return FakeSession(config.apiKey) { key -> closedKeys.update { it + key } }
        }
    }

    @Test
    fun `config change closes old session and rebuilds`() = runTest {
        val keyA = "key-A"
        val keyB = "key-B"
        val store = FakeConfigStore(AiCoachConfig(apiKey = keyA))
        val provider = AiCoachConfigProvider(store)

        // Wait until the provider has settled on A, then construct the executor:
        // its observer sees A as the *first* emission, which drop(1) discards
        // (the initial session is seeded lazily by the first request instead).
        awaitUntil { provider.config.value.apiKey == keyA }
        val factory = FakeSessionFactory()
        val executor = KoogPlanExecutor(configProvider = provider, sessionFactory = factory)

        // Seed the initial session synchronously via a request (fake session,
        // the returned failure is irrelevant).
        executor.request("seed", "system", "user", PartPlanReply.serializer())
        assertEquals(1, factory.createdCount.value)
        assertEquals(listOf(keyA), factory.createdKeys.value)
        assertEquals(emptyList<String>(), factory.closedKeys.value)

        // Model-only change (same apiKey/baseUrl) must NOT rebuild.
        store.save(AiCoachConfig(apiKey = keyA, model = AiCoachModel.DeepSeekV4Pro))
        awaitUntil { provider.config.value.model == AiCoachModel.DeepSeekV4Pro }

        // apiKey change -> old session (A) is closed, a new one (B) is created.
        store.save(AiCoachConfig(apiKey = keyB))
        awaitUntil { factory.createdCount.value >= 2 && factory.closedKeys.value.contains(keyA) }
        assertEquals(2, factory.createdCount.value)
        assertEquals(listOf(keyA, keyB), factory.createdKeys.value)
        assertEquals(listOf(keyA), factory.closedKeys.value)

        // Clearing the apiKey destroys the cached session (B) without creating a new one.
        store.save(AiCoachConfig(apiKey = ""))
        awaitUntil { factory.closedKeys.value.contains(keyB) }
        assertEquals(2, factory.createdCount.value)
        assertEquals(listOf(keyA, keyB), factory.closedKeys.value)

        executor.close()
    }
}
