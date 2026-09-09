package site.xiaozk.dailyfitness.aicoach.prompt

import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.TestActionSpec
import site.xiaozk.dailyfitness.aicoach.TestDayAction
import site.xiaozk.dailyfitness.aicoach.TestPartSpec
import site.xiaozk.dailyfitness.aicoach.TestSetSpec
import site.xiaozk.dailyfitness.aicoach.daysAgo
import site.xiaozk.dailyfitness.aicoach.trainGroups
import site.xiaozk.dailyfitness.aicoach.workoutOf

class AiPromptsTest {

    @Test
    fun `catalog lists parts with type markers`() {
        val groups = trainGroups(
            listOf(
                TestPartSpec(
                    "胸部",
                    listOf(TestActionSpec("卧推", weighted = true, counted = true), TestActionSpec("平板支撑", timed = true)),
                ),
                TestPartSpec("背部", listOf(TestActionSpec("引体向上", counted = true))),
            )
        )
        val text = AiPrompts.formatCatalog(groups)
        assertTrue(text.contains("【动作库】"))
        assertTrue(text.contains("胸部: 卧推(负重+计数)、平板支撑(计时)"))
        assertTrue(text.contains("背部: 引体向上(计数)"))
    }

    @Test
    fun `part plan user prompt shows empty history note when none`() {
        val groups = trainGroups(listOf(TestPartSpec("胸部", listOf(TestActionSpec("卧推", weighted = true, counted = true)))))
        val text = AiPrompts.partPlanUser(groups, emptyList())
        assertTrue(text.contains("【训练历史】空"))
        assertTrue(text.contains("首次推荐"))
    }

    @Test
    fun `part plan prompt renders numbered history and appends additional note`() {
        val groups = trainGroups(listOf(TestPartSpec("胸部", listOf(TestActionSpec("卧推", weighted = true, counted = true)))))
        val sessions = listOf(
            HistorySummarizer.summarize(
                workoutOf(
                    daysAgo(3),
                    TestDayAction("胸部", "卧推", weighted = true, counted = true,
                        sets = listOf(TestSetSpec(weight = 60.0, reps = 8))),
                ),
                daysAgo(0),
            )!!,
            HistorySummarizer.summarize(
                workoutOf(
                    daysAgo(7),
                    TestDayAction("胸部", "卧推", weighted = true, counted = true,
                        sets = listOf(TestSetSpec(weight = 62.5, reps = 6))),
                ),
                daysAgo(0),
            )!!,
        )
        val text = AiPrompts.partPlanUser(groups, sessions, "【补充说明】无法再提供更多训练历史")
        assertTrue(text.contains("【训练历史】共 2 个训练日"))
        assertTrue(text.contains("3天前"))
        assertTrue(text.contains("7天前"))
        assertTrue(text.contains("第1次"))
        assertTrue(text.contains("60kg×8"))
        assertTrue(text.contains("62.5kg×6"))
        assertTrue(text.contains("无法再提供更多训练历史"))
    }

    @Test
    fun `next advice prompt shows today session part history and gap`() {
        val groups = trainGroups(listOf(TestPartSpec("胸部", listOf(TestActionSpec("卧推", weighted = true, counted = true)))))
        val todaySession = HistorySummarizer.summarize(
            workoutOf(
                daysAgo(0),
                TestDayAction("胸部", "卧推", weighted = true, counted = true,
                    sets = listOf(TestSetSpec(weight = 60.0, reps = 8), TestSetSpec(weight = 62.5, reps = 6))),
            ),
            daysAgo(0),
        )!!
        val partHistory = listOf(
            HistorySummarizer.summarize(
                workoutOf(daysAgo(4), TestDayAction("胸部", "卧推", weighted = true, counted = true)),
                daysAgo(0),
            )!!,
        )
        val text = AiPrompts.nextAdviceUser(groups, todaySession, partHistory, lastPartDaysAgo = 4)
        assertTrue(text.contains("【今日已练内容】"))
        assertTrue(text.contains("【该部位近期历史】距上次练该部位：4天前"))
        assertTrue(text.contains("4天前"))
        assertTrue(text.contains("卧推"))
    }

    @Test
    fun `next advice prompt handles never trained part`() {
        val groups = trainGroups(listOf(TestPartSpec("胸部", listOf(TestActionSpec("卧推", weighted = true, counted = true)))))
        val todaySession = HistorySummarizer.summarize(
            workoutOf(daysAgo(0), TestDayAction("胸部", "卧推", weighted = true, counted = true)),
            daysAgo(0),
        )!!
        val text = AiPrompts.nextAdviceUser(groups, todaySession, partHistory = emptyList(), lastPartDaysAgo = null)
        assertTrue(text.contains("今天之前从未练过该部位"))
    }
}
