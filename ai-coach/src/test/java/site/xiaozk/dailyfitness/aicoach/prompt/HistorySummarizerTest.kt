package site.xiaozk.dailyfitness.aicoach.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.TestDayAction
import site.xiaozk.dailyfitness.aicoach.TestSetSpec
import site.xiaozk.dailyfitness.aicoach.daysAgo
import site.xiaozk.dailyfitness.aicoach.todayLocalDate
import site.xiaozk.dailyfitness.aicoach.workoutOf
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit

class HistorySummarizerTest {

    private val today = todayLocalDate()

    @Test
    fun `empty workout yields null`() {
        assertNull(HistorySummarizer.summarize(workoutOf(daysAgo(1)), today))
    }

    @Test
    fun `groups sets by part and action`() {
        val workout = workoutOf(
            daysAgo(1),
            TestDayAction("胸部", "卧推", weighted = true, counted = true,
                sets = listOf(TestSetSpec(weight = 60.0, reps = 8), TestSetSpec(weight = 60.0, reps = 6))),
            TestDayAction("背部", "引体向上", counted = true, sets = listOf(TestSetSpec(reps = 10))),
        )
        val summary = HistorySummarizer.summarize(workout, today)
        assertNotNull(summary)
        assertEquals(listOf("胸部", "背部"), summary!!.parts.map { it.partName })
        val chest = summary.parts.first()
        assertEquals("卧推", chest.actions.single().actionName)
        assertEquals(2, chest.actions.single().sets.size)
        assertEquals(1, summary.daysAgo)
    }

    @Test
    fun `normalises lbs to kg and minutes to seconds`() {
        val workout = workoutOf(
            daysAgo(3),
            TestDayAction("胸部", "卧推", weighted = true, counted = true, timed = true,
                sets = listOf(
                    TestSetSpec(weight = 100.0, weightUnit = WeightUnit.Lbs, reps = 10, duration = 2, durationUnit = TimeUnit.Min),
                )),
        )
        val set = HistorySummarizer.summarize(workout, today)!!.parts.single().actions.single().sets.single()
        assertEquals(100.0 * WeightUnit.Lbs.trans.toDouble(), set.weightKg!!, 0.001)
        assertEquals(120, set.durationSec)
        assertEquals(3, HistorySummarizer.summarize(workout, today)!!.daysAgo)
    }

    @Test
    fun `drops zero values and keeps null fields`() {
        val workout = workoutOf(
            daysAgo(2),
            TestDayAction("腿", "深蹲", weighted = true, counted = true,
                sets = listOf(TestSetSpec(weight = 0.0, reps = 0), TestSetSpec(weight = 80.0, reps = 5))),
        )
        val sets = HistorySummarizer.summarize(workout, today)!!.parts.single().actions.single().sets
        assertNull(sets[0].weightKg)
        assertNull(sets[0].reps)
        assertEquals(80.0, sets[1].weightKg!!, 0.001)
        assertEquals(5, sets[1].reps)
    }

    @Test
    fun `truncates long set lists with a marker`() {
        val sets = (1..12).map { TestSetSpec(weight = 60.0, reps = 8) }
        val workout = workoutOf(
            daysAgo(1),
            TestDayAction("胸部", "卧推", weighted = true, counted = true, sets = sets),
        )
        val action = HistorySummarizer.summarize(workout, today)!!.parts.single().actions.single()
        assertTrue(action.truncated)
        assertEquals(MAX_SETS_PER_ACTION, action.sets.size)
    }

    @Test
    fun `containsPart and partOnly filtering`() {
        val workout = workoutOf(
            daysAgo(1),
            TestDayAction("胸部", "卧推", weighted = true, counted = true),
            TestDayAction("背部", "划船", counted = true),
        )
        assertTrue(HistorySummarizer.containsPart(workout, "胸部"))
        assertFalse(HistorySummarizer.containsPart(workout, "肩部"))

        val onlyChest = HistorySummarizer.summarizePartOnly(workout, "胸部", today)
        assertEquals(listOf("胸部"), onlyChest!!.parts.map { it.partName })

        val other = HistorySummarizer.summarizePartOnly(workout, "肩部", today)
        assertNull(other)
    }
}
