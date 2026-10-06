package site.xiaozk.dailyfitness.aicoach.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.FakeA2uiCapabilityProvider
import site.xiaozk.dailyfitness.aicoach.FakeConfigStore
import site.xiaozk.dailyfitness.aicoach.FakePlanExecutor
import site.xiaozk.dailyfitness.aicoach.FakeTrainActionRepository
import site.xiaozk.dailyfitness.aicoach.FakeUserRepository
import site.xiaozk.dailyfitness.aicoach.FakeWorkoutRepository
import site.xiaozk.dailyfitness.aicoach.TestDayAction
import site.xiaozk.dailyfitness.aicoach.TestSetSpec
import site.xiaozk.dailyfitness.aicoach.chestGroups
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.daysAgo
import site.xiaozk.dailyfitness.aicoach.llm.PlanExecutor
import site.xiaozk.dailyfitness.aicoach.workoutMap
import site.xiaozk.dailyfitness.aicoach.workoutOf
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup

/**
 * Tests of the agent-driven (A2UI) engine branch.
 *
 * The negotiation loop lives entirely inside `recommendToday`, so these tests cover both the
 * retry policy (the agent asking for more history) and the fact that only the final answer
 * ever leaves the engine.
 */
class AiCoachEngineAgentUiTest {

    private val configured = AiCoachConfig(apiKey = FAKE_API_KEY)

    private suspend fun newEngine(
        config: AiCoachConfig = configured,
        map: List<DailyWorkout> = workoutMap(),
        groups: List<TrainPartGroup>? = null,
        executor: PlanExecutor = FakePlanExecutor(),
        capability: FakeA2uiCapabilityProvider = FakeA2uiCapabilityProvider(),
    ): AiCoachEngine {
        val resolvedGroups = groups ?: chestGroups()
        val store = FakeConfigStore(config)
        val provider = AiCoachConfigProvider(store)
        provider.config.first {
            it.apiKey == config.apiKey && it.model == config.model &&
                it.baseUrl == config.baseUrl && it.timeoutSeconds == config.timeoutSeconds
        }
        return AiCoachEngine(
            configProvider = provider,
            userRepository = FakeUserRepository(),
            workoutRepository = FakeWorkoutRepository(map),
            trainRepository = FakeTrainActionRepository(resolvedGroups),
            planExecutor = executor,
            coachLocaleProvider = CoachLocaleProvider { "en-US" },
            a2uiCapabilityProvider = capability,
        )
    }

    // --------------------------------------------------------------- happy path

    @Test
    fun `case A renders the surface authored by the agent`() = runTest {
        val executor = FakePlanExecutor().apply { enqueueRawText(Result.success(UI_REPLY)) }
        val engine = newEngine(executor = executor)

        val result = engine.recommendToday(emptyList())

        val agentUi = result as? AiCoachResult.AgentUi ?: error("expected AgentUi, got $result")
        assertEquals(SURFACE_ID, agentUi.surfaceId)
        assertEquals(2, agentUi.messages.size)
        val content = agentUi.assistantMessage.content
        assertTrue(content is CoachMessageContent.AgentUi)
        assertEquals(1, executor.calls.size)
    }

    @Test
    fun `the system prompt carries the catalog id and its schema`() = runTest {
        val capability = FakeA2uiCapabilityProvider(schema = """{"components":{"PartCard":{}}}""")
        val executor = FakePlanExecutor().apply { enqueueRawText(Result.success(UI_REPLY)) }
        val engine = newEngine(executor = executor, capability = capability)

        engine.recommendToday(emptyList())

        val systemText = executor.systemTexts.single()
        assertTrue(systemText.contains(capability.catalogId))
        assertTrue(systemText.contains("""{"components":{"PartCard":{}}}"""))
        assertTrue(systemText.contains("needMore"))
    }

    @Test
    fun `case B renders the advice surface`() = runTest {
        val executor = FakePlanExecutor().apply { enqueueRawText(Result.success(UI_REPLY)) }
        val engine = newEngine(map = todayChestWorkout(), executor = executor)

        val result = engine.recommendToday(emptyList())

        assertTrue(result is AiCoachResult.AgentUi)
        assertEquals("aicoach-agent-advice", executor.calls.single().first)
    }

    // ------------------------------------------------------------- negotiation

    @Test
    fun `needMore triggers another round and the surface of the final round is the answer`() =
        runTest {
            val executor = FakePlanExecutor().apply {
                enqueueRawText(Result.success("""{"control":"needMore","wantSessions":5}"""))
                enqueueRawText(Result.success(UI_REPLY))
            }
            // Only 3 sessions, so the initial window already covers everything: the second
            // round is told that no more history can be provided.
            val engine = newEngine(map = history(3), executor = executor)

            val result = engine.recommendToday(emptyList())

            assertTrue(result is AiCoachResult.AgentUi)
            assertEquals(2, executor.calls.size)
            assertTrue(
                executor.agentRequests.last().contains("No more training history can be provided")
            )
        }

    @Test
    fun `needMore with no history at all fails fast`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueueRawText(Result.success("""{"control":"needMore","wantSessions":3}"""))
        }
        val engine = newEngine(groups = chestGroups(), executor = executor)

        val result = engine.recommendToday(emptyList())

        val failed = result as? AiCoachResult.Failed ?: error("expected Failed, got $result")
        assertEquals(CoachFailure.NeedMoreWithoutHistory, failed.failure)
        assertTrue(failed.retryable)
    }

    @Test
    fun `a persistent needMore exhausts the negotiation`() = runTest {
        val executor = FakePlanExecutor().apply {
            repeat(6) { enqueueRawText(Result.success("""{"control":"needMore","wantSessions":1}""")) }
        }
        val engine = newEngine(map = history(25), executor = executor)

        val result = engine.recommendToday(emptyList())

        val failed = result as? AiCoachResult.Failed ?: error("expected Failed, got $result")
        assertEquals(CoachFailure.InsufficientHistory, failed.failure)
    }

    // ------------------------------------------------------------------ errors

    @Test
    fun `an unparseable agent reply is reported as a model error`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueueRawText(Result.success("I am not going to answer that."))
        }
        val engine = newEngine(executor = executor)

        val result = engine.recommendToday(emptyList())

        val failed = result as? AiCoachResult.Failed ?: error("expected Failed, got $result")
        assertTrue(failed.failure is CoachFailure.ModelError)
    }

    @Test
    fun `a reply that never creates a surface is reported as a model error`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueueRawText(
                Result.success("""{"version":"v0.9","updateComponents":{"surfaceId":"s","components":[]}}""")
            )
        }
        val engine = newEngine(executor = executor)

        val result = engine.recommendToday(emptyList())

        val failed = result as? AiCoachResult.Failed ?: error("expected Failed, got $result")
        assertTrue(failed.failure is CoachFailure.ModelError)
    }

    @Test
    fun `a transport failure is mapped to a user facing error`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueueRawText(Result.failure(IllegalStateException("401 Unauthorized")))
        }
        val engine = newEngine(executor = executor)

        val result = engine.recommendToday(emptyList())

        val failed = result as? AiCoachResult.Failed ?: error("expected Failed, got $result")
        assertEquals(CoachFailure.InvalidKey, failed.failure)
    }

    @Test
    fun `the whole exchange shares one timeout budget`() = runTest {
        // runTest's virtual clock makes the delay deterministic: the single 5s budget fires
        // long before the 60s the fake would take.
        val neverEnding = object : PlanExecutor {
            override suspend fun <T> request(
                promptId: String,
                systemText: String,
                userText: String,
                history: List<CoachMessage>,
                serializer: KSerializer<T>,
            ): Result<T> = error("unused")

            override suspend fun requestRawText(
                promptId: String,
                systemText: String,
                userText: String,
                history: List<CoachMessage>,
            ): Result<String> {
                delay(60_000)
                error("must not be reached")
            }

            override fun close() = Unit
        }
        val engine = newEngine(
            config = AiCoachConfig(apiKey = FAKE_API_KEY, timeoutSeconds = 5),
            executor = neverEnding,
        )

        val result = engine.recommendToday(emptyList())

        val failed = result as? AiCoachResult.Failed ?: error("expected Failed, got $result")
        assertEquals(CoachFailure.Timeout, failed.failure)
        assertTrue(failed.retryable)
    }

    @Test
    fun `config missing still short-circuits before any request`() = runTest {
        val executor = FakePlanExecutor()
        val engine = newEngine(config = AiCoachConfig(), executor = executor)

        assertEquals(AiCoachResult.ConfigMissing, engine.recommendToday(emptyList()))
        assertTrue(executor.calls.isEmpty())
    }

    private fun todayChestWorkout() = workoutMap(
        workoutOf(
            daysAgo(0),
            TestDayAction(
                "胸部", "卧推",
                weighted = true, counted = true,
                sets = listOf(TestSetSpec(weight = 60.0, reps = 8)),
            ),
        ),
    )

    private fun history(days: Int): List<DailyWorkout> = workoutMap(
        *(1..days).map { day ->
            workoutOf(
                daysAgo(day),
                TestDayAction(
                    "胸部", "卧推",
                    weighted = true, counted = true,
                    sets = listOf(TestSetSpec(weight = 60.0, reps = 8)),
                ),
            )
        }.toTypedArray(),
    )

    private companion object {
        /** Marker only - never sent anywhere; the LLM seam is a fake. */
        const val FAKE_API_KEY = "fake-key-for-fake-executor-tests"
        const val SURFACE_ID = "plan-1"
        const val CATALOG_ID = "https://dailyfitness.xiaozk.site/a2ui/v1/catalog.json"

        val UI_REPLY = """
            {"version":"v0.9","createSurface":{"surfaceId":"$SURFACE_ID","catalogId":"$CATALOG_ID","sendDataModel":false}}
            {"version":"v0.9","updateComponents":{"surfaceId":"$SURFACE_ID","components":[{"id":"root","component":"PartCard","partName":"Chest","children":[]}]}}
        """.trimIndent()
    }
}
