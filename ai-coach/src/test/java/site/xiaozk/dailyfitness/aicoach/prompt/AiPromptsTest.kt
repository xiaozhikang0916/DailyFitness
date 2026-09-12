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
        assertTrue(text.contains("[Exercise Catalog]"))
        assertTrue(text.contains("胸部: 卧推(weighted+counted), 平板支撑(timed)"))
        assertTrue(text.contains("背部: 引体向上(counted)"))
    }

    @Test
    fun `part plan user prompt shows empty history note when none`() {
        val groups = trainGroups(listOf(TestPartSpec("胸部", listOf(TestActionSpec("卧推", weighted = true, counted = true)))))
        val text = AiPrompts.partPlanUser(groups, emptyList())
        assertTrue(text.contains("[Training History] empty"))
        assertTrue(text.contains("first-time recommendation"))
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
        val text = AiPrompts.partPlanUser(groups, sessions, "No more training history can be provided")
        assertTrue(text.contains("[Training History] 2 training day(s)"))
        assertTrue(text.contains("3 days ago"))
        assertTrue(text.contains("7 days ago"))
        assertTrue(text.contains("Session 1"))
        assertTrue(text.contains("60kg×8"))
        assertTrue(text.contains("62.5kg×6"))
        assertTrue(text.contains("[Additional Note] No more training history can be provided"))
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
        assertTrue(text.contains("[Today's Completed Sets]"))
        assertTrue(text.contains("[Recent History for This Part] last trained this part: 4 days ago"))
        assertTrue(text.contains("4 days ago"))
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
        assertTrue(text.contains("never been trained before today"))
    }

    @Test
    fun `system prompts append the locale output-language instruction`() {
        val partPlan = AiPrompts.partPlanSystem("en-US")
        val nextAdvice = AiPrompts.nextAdviceSystem("zh-CN")
        assertTrue(partPlan.contains("The user's locale is \"en-US\""))
        assertTrue(nextAdvice.contains("The user's locale is \"zh-CN\""))
        assertTrue(partPlan.contains("must never be translated"))
    }
}
