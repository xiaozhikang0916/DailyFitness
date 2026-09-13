package site.xiaozk.dailyfitness.aicoach.engine

import ai.koog.http.client.KoogHttpClient
import ai.koog.http.client.ktor.KtorKoogHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.FakeConfigStore
import site.xiaozk.dailyfitness.aicoach.FakeTrainActionRepository
import site.xiaozk.dailyfitness.aicoach.FakeUserRepository
import site.xiaozk.dailyfitness.aicoach.FakeWorkoutRepository
import site.xiaozk.dailyfitness.aicoach.TestDayAction
import site.xiaozk.dailyfitness.aicoach.TestSetSpec
import site.xiaozk.dailyfitness.aicoach.awaitUntil
import site.xiaozk.dailyfitness.aicoach.chestGroups
import site.xiaozk.dailyfitness.aicoach.daysAgo
import site.xiaozk.dailyfitness.aicoach.workoutMap
import site.xiaozk.dailyfitness.aicoach.workoutOf
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.llm.KoogPlanExecutor
import site.xiaozk.dailyfitness.aicoach.llm.RealLlmSessionFactory
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutMap

/**
 * Real-LLM integration tests: run the engine scenarios against DeepSeek through
 * the production [KoogPlanExecutor] / [RealLlmSessionFactory], reusing the same
 * fixtures and scenario shapes as the fake-executor suite.
 *
 * The API key MUST be supplied externally (no default):
 *   ./gradlew :ai-coach:testDebugUnitTest --tests '*AiCoachRealLlmTest*' -Pdeepseek.apiKey=sk-...
 * or via the DEEPSEEK_API_KEY env var. When it is missing, every test in this
 * class is skipped (JUnit assumption in @Before), never failed.
 */
class AiCoachRealLlmTest {

    private val apiKey: String? = System.getProperty("deepseek.apiKey")
        ?: System.getenv("DEEPSEEK_API_KEY")

    @Before
    fun skipWithoutKey() {
        assumeFalse(
            "真实 LLM 测试需要外部传入 API Key（无默认值）：-Pdeepseek.apiKey=... 或 DEEPSEEK_API_KEY",
            apiKey.isNullOrBlank(),
        )
    }

    private fun httpClientFactory(): KoogHttpClient.Factory =
        KtorKoogHttpClient.Factory(
            baseClient = HttpClient(OkHttp),
            withSse = false,
        )

    private suspend fun newRealEngine(map: DailyWorkoutMap = workoutMap()): AiCoachEngine {
        val config = AiCoachConfig(apiKey = requireNotNull(apiKey))
        val store = FakeConfigStore(config)
        val provider = AiCoachConfigProvider(store)
        awaitUntil { provider.config.value.apiKey == apiKey }
        val executor = KoogPlanExecutor(
            configProvider = provider,
            sessionFactory = RealLlmSessionFactory(httpClientFactory()),
        )
        return AiCoachEngine(
            configProvider = provider,
            userRepository = FakeUserRepository(),
            workoutRepository = FakeWorkoutRepository(map),
            trainRepository = FakeTrainActionRepository(chestGroups()),
            planExecutor = executor,
            coachLocaleProvider = CoachLocaleProvider { "zh-CN" },
        )
    }

    @Test
    fun `real LLM - case A without history returns a matched today plan`() = runTest {
        val result = withContext(Dispatchers.Default) { newRealEngine().recommendToday(emptyList()) }
        assertTrue("expected TodayPlan, got $result", result is AiCoachResult.TodayPlan)
    }

    @Test
    fun `real LLM - case B with today session returns next advice`() = runTest {
        val todayWorkout = workoutOf(
            daysAgo(0),
            TestDayAction(
                "胸部", "卧推",
                weighted = true, counted = true,
                sets = listOf(TestSetSpec(weight = 60.0, reps = 8), TestSetSpec(weight = 62.5, reps = 6)),
            ),
        )
        val result = withContext(Dispatchers.Default) {
            newRealEngine(map = workoutMap(todayWorkout)).recommendToday(emptyList())
        }
        assertTrue("expected NextAdvice, got $result", result is AiCoachResult.NextAdvice)
    }
}
