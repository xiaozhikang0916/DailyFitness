package site.xiaozk.dailyfitness.session

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListActionPair
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPart

class WorkoutSessionStateMachineTest {

    private val part = TrainPart(id = 1, partName = "chest")
    private val pushUp = TrainAction(
        id = 10,
        partId = 1,
        actionName = "push-up",
        isCountedAction = true,
    )
    private val sitUp = TrainAction(
        id = 11,
        partId = 1,
        actionName = "sit-up",
        isCountedAction = true,
    )

    private fun set(instant: Instant, action: TrainAction) = DailyWorkoutAction(
        id = instant.toEpochMilliseconds().toInt(),
        instant = instant,
        action = action,
        takenDuration = null,
        takenWeight = null,
        takenCount = 1,
        note = "",
    )

    private fun workoutOf(vararg pairs: Pair<TrainAction, List<Instant>>): DailyWorkout {
        val actions = pairs.map { (action, instants) ->
            DailyWorkoutListActionPair(
                TrainActionWithPart(part = part, action = action),
                instants.map { set(it, action) },
            )
        }
        return DailyWorkout(date = LocalDate(2025, 1, 1), actions = actions)
    }

    private val t0 = Instant.fromEpochMilliseconds(1_000L)
    private val t1 = Instant.fromEpochMilliseconds(2_000L)
    private val t2 = Instant.fromEpochMilliseconds(3_000L)

    @Test
    fun `inactive session yields default state`() {
        val state = deriveSessionState(SessionMeta(), workoutOf(pushUp to listOf(t0)))
        assertFalse(state.active)
        assertNull(state.startedAt)
        assertEquals(0, state.setsDone)
        assertNull(state.currentActionId)
    }

    @Test
    fun `active session without any record yields active empty state`() {
        val meta = SessionMeta(active = true, startedAt = t0)
        val state = deriveSessionState(meta, null)
        assertTrue(state.active)
        assertEquals(t0, state.startedAt)
        assertNull(state.currentActionId)
        assertEquals(0, state.setsDone)
    }

    @Test
    fun `active session derives current action and set count`() {
        val meta = SessionMeta(active = true, startedAt = t0)
        val state = deriveSessionState(
            meta,
            workoutOf(pushUp to listOf(t0, t1), sitUp to listOf(t2)),
        )
        assertTrue(state.active)
        assertEquals(sitUp.id, state.currentActionId)
        assertEquals("sit-up", state.currentActionName)
        assertEquals(1, state.setsDone)
        assertEquals(3, state.totalSetsToday)
    }

    @Test
    fun `current action follows the latest recorded set across actions`() {
        val meta = SessionMeta(active = true, startedAt = t0)
        val state = deriveSessionState(
            meta,
            workoutOf(pushUp to listOf(t2), sitUp to listOf(t0)),
        )
        assertEquals(pushUp.id, state.currentActionId)
        assertEquals(1, state.setsDone)
    }

    @Test
    fun `sets of the latest action are counted`() {
        val meta = SessionMeta(active = true, startedAt = t0)
        val state = deriveSessionState(
            meta,
            workoutOf(pushUp to listOf(t0, t1, t2)),
        )
        assertEquals(pushUp.id, state.currentActionId)
        assertEquals(3, state.setsDone)
        assertEquals(3, state.totalSetsToday)
    }
}
