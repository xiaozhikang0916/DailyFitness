package site.xiaozk.dailyfitness.aicoach.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.FakeConfigStore
import site.xiaozk.dailyfitness.aicoach.FakePlanExecutor
import site.xiaozk.dailyfitness.aicoach.FakeTrainActionRepository
import site.xiaozk.dailyfitness.aicoach.FakeUserRepository
import site.xiaozk.dailyfitness.aicoach.FakeWorkoutRepository
import site.xiaozk.dailyfitness.aicoach.TestActionSpec
import site.xiaozk.dailyfitness.aicoach.TestDayAction
import site.xiaozk.dailyfitness.aicoach.TestPartSpec
import site.xiaozk.dailyfitness.aicoach.TestSetSpec
import site.xiaozk.dailyfitness.aicoach.chestGroups
import site.xiaozk.dailyfitness.aicoach.daysAgo
import site.xiaozk.dailyfitness.aicoach.trainGroups
import site.xiaozk.dailyfitness.aicoach.workoutMap
import site.xiaozk.dailyfitness.aicoach.workoutOf
import site.xiaozk.dailyfitness.aicoach.llm.ActionPlan
import site.xiaozk.dailyfitness.aicoach.llm.AdviceKindReply
import site.xiaozk.dailyfitness.aicoach.llm.NextAdviceReply
import site.xiaozk.dailyfitness.aicoach.llm.PartPlan
import site.xiaozk.dailyfitness.aicoach.llm.PartPlanReply
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutMap
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup

class AiCoachEngineTest {

    // Fake-executor tests never hit the network, so the apiKey only needs to pass
    // the `configured` gate. A clearly-marked internal token is used instead of an
    // external key (real-LLM tests would use -Pdeepseek.apiKey / DEEPSEEK_API_KEY).
    private val configured = AiCoachConfig(apiKey = FAKE_API_KEY)

    private suspend fun newEngine(
        config: AiCoachConfig = configured,
        map: DailyWorkoutMap = workoutMap(),
        groups: List<TrainPartGroup>? = null,
        executor: FakePlanExecutor = FakePlanExecutor(),
    ): AiCoachEngine {
        val resolvedGroups = groups ?: chestGroups()
        val store = FakeConfigStore(config)
        val provider = AiCoachConfigProvider(store)
        // Propagation is async (provider scope on Dispatchers.Default); wait for the real
        // emission instead of bounding with virtual-time timeouts (runTest has none here).
        provider.config.first {
            it.apiKey == config.apiKey && it.model == config.model && it.baseUrl == config.baseUrl
        }
        return AiCoachEngine(
            configProvider = provider,
            userRepository = FakeUserRepository(),
            workoutRepository = FakeWorkoutRepository(map),
            trainRepository = FakeTrainActionRepository(resolvedGroups),
            planExecutor = executor,
        )
    }

    private fun chestPlan(): PartPlanReply = PartPlanReply(
        needMore = false,
        plan = listOf(
            PartPlan(
                partName = "胸部",
                isPrimary = true,
                reason = "上周未练胸部",
                actions = listOf(
                    ActionPlan(actionName = "卧推", sets = 4, reps = 8, weightKg = 60.0),
                    ActionPlan(actionName = "哑铃飞鸟", sets = 3, reps = 10, weightKg = 12.5),
                ),
            )
        ),
    )

    // ------------------------------------------------------------- config/gates

    @Test
    fun `config missing returns ConfigMissing`() = runTest {
        val engine = newEngine(config = AiCoachConfig())
        assertEquals(AiCoachResult.ConfigMissing, engine.recommendToday(emptyList()))
    }

    @Test
    fun `no train parts returns NoTrainParts`() = runTest {
        val engine = newEngine(groups = emptyList())
        assertEquals(AiCoachResult.NoTrainParts, engine.recommendToday(emptyList()))
    }

    // ------------------------------------------------------------- case A

    @Test
    fun `case A with no history returns today plan from a single call`() = runTest {
        val executor = FakePlanExecutor().apply { enqueuePartPlan(Result.success(chestPlan())) }
        val groups = trainGroups(
            listOf(
                TestPartSpec(
                    "胸部",
                    listOf(
                        TestActionSpec("卧推", weighted = true, counted = true),
                        TestActionSpec("哑铃飞鸟", weighted = true, counted = true),
                    ),
                )
            )
        )
        val engine = newEngine(map = workoutMap(), groups = groups, executor = executor)

        val result = engine.recommendToday(emptyList())

        val plan = result as? AiCoachResult.TodayPlan ?: error("expected TodayPlan")
        assertEquals(0, plan.sessionsUsed)
        assertEquals(1, plan.rounds)
        assertEquals("胸部", plan.parts.single().partName)
        assertEquals(listOf("卧推", "哑铃飞鸟"), plan.parts.single().actions.map { it.actionName })
        assertEquals(listOf(4, 3), plan.parts.single().actions.map { it.sets })
        assertEquals(emptyList<String>(), plan.ignoredNames)
        assertEquals(1, executor.calls.size)
        assertTrue(executor.partPlanRequests.single().contains("【训练历史】空"))
    }

    @Test
    fun `case A expands history on needMore and falls back when it cannot grow`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueuePartPlan(Result.success(PartPlanReply(needMore = true, wantSessions = 5)))
            enqueuePartPlan(Result.success(chestPlan()))
        }
        // Only 3 sessions: initial window already covers everything -> fallback note.
        val history = (1..3).map { day -> workoutOf(daysAgo(day), TestDayAction("胸部", "卧推", weighted = true, counted = true, sets = listOf(TestSetSpec(weight = 60.0, reps = 8)))) }
        val engine = newEngine(map = workoutMap(*history.toTypedArray()), executor = executor)

        val result = engine.recommendToday(emptyList())

        val plan = result as? AiCoachResult.TodayPlan ?: error("expected TodayPlan")
        assertEquals(3, plan.sessionsUsed)
        assertEquals(2, executor.calls.size)
        assertTrue(executor.partPlanRequests.last().contains("无法再提供更多训练历史"))
    }

    @Test
    fun `case A caps expansion rounds and forces a final answer`() = runTest {
        val executor = FakePlanExecutor().apply {
            repeat(3) { enqueuePartPlan(Result.success(PartPlanReply(needMore = true, wantSessions = 1))) }
            enqueuePartPlan(Result.success(chestPlan()))
        }
        val history = (1..25).map { day -> workoutOf(daysAgo(day), TestDayAction("胸部", "卧推", weighted = true, counted = true, sets = listOf(TestSetSpec(weight = 60.0, reps = 8)))) }
        val engine = newEngine(map = workoutMap(*history.toTypedArray()), executor = executor)

        val result = engine.recommendToday(emptyList())

        val plan = result as? AiCoachResult.TodayPlan ?: error("expected TodayPlan")
        assertEquals(4, executor.calls.size)
        assertTrue(plan.rounds >= 3)
        assertTrue(executor.partPlanRequests.last().contains("无法再提供更多训练历史"))
    }

    @Test
    fun `case A maps unmatched names to Failed`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueuePartPlan(
                Result.success(
                    PartPlanReply(
                        needMore = false,
                        plan = listOf(PartPlan(partName = "不存在的部位", actions = listOf(ActionPlan(actionName = "卧推", sets = 3)))),
                    )
                )
            )
        }
        val engine = newEngine(executor = executor)
        val result = engine.recommendToday(emptyList()) as? AiCoachResult.Failed
            ?: error("expected Failed")
        assertTrue(result.message.contains("无法匹配"))
        assertTrue(result.retryable)
    }

    @Test
    fun `request failure is mapped to user facing error`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueuePartPlan(Result.failure(RuntimeException("Connection timed out")))
        }
        val engine = newEngine(executor = executor)
        val result = engine.recommendToday(emptyList()) as? AiCoachResult.Failed
            ?: error("expected Failed")
        assertTrue(result.message.contains("网络连接失败"))
        assertTrue(result.retryable)
    }

    // ------------------------------------------------------------- case B

    private fun todayChestWorkout() = workoutMap(
        workoutOf(
            daysAgo(0),
            TestDayAction(
                "胸部", "卧推",
                weighted = true, counted = true,
                sets = listOf(TestSetSpec(weight = 60.0, reps = 8), TestSetSpec(weight = 62.5, reps = 6)),
            ),
        )
    )

    @Test
    fun `case B continue current action maps validated params`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueueNextAdvice(
                Result.success(
                    NextAdviceReply(
                        kind = AdviceKindReply.CONTINUE_CURRENT,
                        actionName = "卧推",
                        sets = 1,
                        reps = 8,
                        weightKg = 62.5,
                        reason = "状态良好，继续加组",
                    )
                )
            )
        }
        val history = workoutMap(
            workoutOf(daysAgo(4), TestDayAction("胸部", "卧推", weighted = true, counted = true)),
            workoutOf(daysAgo(9), TestDayAction("胸部", "卧推", weighted = true, counted = true)),
        )
        val map = workoutMap(
            *history.trainedDate.values.toTypedArray(),
            *todayChestWorkout().trainedDate.values.toTypedArray(),
        )
        val engine = newEngine(map = map, executor = executor)

        val result = engine.recommendToday(emptyList())
        val next = result as? AiCoachResult.NextAdvice ?: error("expected NextAdvice")
        val advice = next.advice
        assertEquals(AdviceKind.CONTINUE_CURRENT, advice.kind)
        assertEquals("卧推", advice.actionName)
        assertEquals(1, advice.sets)
        assertEquals(8, advice.reps)
        assertEquals(62.5, advice.weightKg!!, 0.001)
        assertEquals(emptyList<String>(), next.ignoredNames)
        val userText = executor.nextAdviceRequests.single()
        assertTrue(userText.contains("【今日已练内容】"))
        assertTrue(userText.contains("距上次练该部位：4天前"))
    }

    @Test
    fun `case B switch action with unknown action fails`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueueNextAdvice(
                Result.success(NextAdviceReply(kind = AdviceKindReply.SWITCH_ACTION, actionName = "不存在动作", sets = 3))
            )
        }
        val engine = newEngine(map = todayChestWorkout(), executor = executor)
        val result = engine.recommendToday(emptyList()) as? AiCoachResult.Failed
            ?: error("expected Failed")
        assertTrue(result.message.contains("不在你的动作库中"))
    }

    @Test
    fun `case B finish day yields zero sets advice`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueueNextAdvice(Result.success(NextAdviceReply(kind = AdviceKindReply.FINISH_DAY, reason = "今天够了")))
        }
        val engine = newEngine(map = todayChestWorkout(), executor = executor)
        val result = engine.recommendToday(emptyList())
        val advice = (result as? AiCoachResult.NextAdvice)?.advice ?: error("expected NextAdvice")
        assertEquals(AdviceKind.FINISH_DAY, advice.kind)
        assertEquals(0, advice.sets)
    }

    @Test
    fun `case A never trained part mentions first-time state`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueueNextAdvice(
                Result.success(NextAdviceReply(kind = AdviceKindReply.CONTINUE_CURRENT, actionName = "卧推", sets = 1, reps = 8))
            )
        }
        // Today trained 胸部 but history contains only 背部.
        val map = workoutMap(
            workoutOf(daysAgo(5), TestDayAction("背部", "划船", counted = true)),
            *todayChestWorkout().trainedDate.values.toTypedArray(),
        )
        val engine = newEngine(map = map, executor = executor)
        val result = engine.recommendToday(emptyList())
        assertTrue(result is AiCoachResult.NextAdvice)
        assertTrue(executor.nextAdviceRequests.single().contains("从未练过该部位"))
    }

    // ------------------------------------------------------- conversation memory

    @Test
    fun `conversation history is forwarded and capped to five rounds`() = runTest {
        val executor = FakePlanExecutor().apply { enqueuePartPlan(Result.success(chestPlan())) }
        val engine = newEngine(executor = executor)
        val conversation = (1..14).map { index ->
            site.xiaozk.dailyfitness.aicoach.engine.CoachMessage(
                fromUser = index % 2 == 1,
                text = "message-$index",
            )
        }

        val result = engine.recommendToday(conversation)

        // Engine keeps only the last 10 messages (5 rounds) and forwards them to the LLM.
        assertEquals(conversation.takeLast(10), executor.histories.single())
        val plan = result as? AiCoachResult.TodayPlan ?: error("expected TodayPlan")
        assertEquals(2, plan.newMessages.size)
        assertTrue(plan.newMessages.first().fromUser)
        assertTrue(plan.newMessages.first().text.contains("Case A"))
        // Structured suggestions for M3.3 prefill: one per planned action.
        val suggestions = plan.newMessages.last().suggestions
        assertEquals(2, suggestions.size)
        assertEquals(listOf("卧推", "哑铃飞鸟"), suggestions.map { it.actionName })
        assertEquals(listOf("胸部", "胸部"), suggestions.map { it.partName })
        assertEquals(60.0, suggestions.first().weightKg!!, 0.001)
        assertEquals(4, suggestions.first().sets)
        assertTrue(plan.newMessages.first().suggestions.isEmpty())
    }

    @Test
    fun `case B result exposes a conversation turn`() = runTest {
        val executor = FakePlanExecutor().apply {
            enqueueNextAdvice(
                Result.success(
                    NextAdviceReply(
                        kind = AdviceKindReply.CONTINUE_CURRENT,
                        actionName = "卧推",
                        sets = 1,
                        reps = 8,
                        weightKg = 62.5,
                    )
                )
            )
        }
        val engine = newEngine(map = todayChestWorkout(), executor = executor)

        val result = engine.recommendToday(emptyList())

        val next = result as? AiCoachResult.NextAdvice ?: error("expected NextAdvice")
        assertEquals(2, next.newMessages.size)
        assertTrue(next.newMessages.first().text.contains("Case B"))
        assertTrue(next.newMessages.last().text.contains("卧推"))
        assertTrue(next.newMessages.last().text.contains("62.5kg"))
        // Single machine-actionable suggestion for the next set (M3.3 prefill).
        val suggestion = next.newMessages.last().suggestions.single()
        assertEquals("胸部", suggestion.partName)
        assertEquals("卧推", suggestion.actionName)
        assertEquals(1, suggestion.sets)
        assertEquals(8, suggestion.reps)
        assertEquals(62.5, suggestion.weightKg!!, 0.001)
    }

    private companion object {
        /** Marker only - never sent anywhere; the LLM seam is a fake. */
        const val FAKE_API_KEY = "fake-key-for-fake-executor-tests"
    }
}
