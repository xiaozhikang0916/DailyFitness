package site.xiaozk.dailyfitness.aicoach.ui.a2ui

import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.a2ui.A2uiActionContract
import site.xiaozk.dailyfitness.aicoach.engine.CoachSuggestion

/**
 * The mapper is the seam between the agent-authored surface vocabulary and the app's own
 * prefill intent, so it must be tolerant about how the agent spelled the values.
 */
class DailyFitnessA2uiActionsTest {

    @Test
    fun `maps an adopt event to a suggestion`() {
        val suggestion = DailyFitnessA2uiActions.suggestionOf(
            adoptEvent(
                mapOf(
                    "partName" to "胸部",
                    "actionName" to "卧推",
                    "sets" to 3,
                    "reps" to 10,
                    "weightKg" to 60.0,
                )
            )
        )

        assertEquals(
            CoachSuggestion(
                partName = "胸部",
                actionName = "卧推",
                sets = 3,
                reps = 10,
                weightKg = 60.0,
                durationSec = null,
            ),
            suggestion,
        )
    }

    @Test
    fun `accepts values the agent spelled as strings`() {
        val suggestion = DailyFitnessA2uiActions.suggestionOf(
            adoptEvent(
                mapOf(
                    "actionName" to "平板支撑",
                    "sets" to "4",
                    "durationSec" to "45",
                )
            )
        )

        assertEquals("平板支撑", suggestion?.actionName)
        assertEquals(4, suggestion?.sets)
        assertEquals(45, suggestion?.durationSec)
        assertEquals(null, suggestion?.weightKg)
    }

    @Test
    fun `drops values that are not numeric`() {
        val suggestion = DailyFitnessA2uiActions.suggestionOf(
            adoptEvent(mapOf("actionName" to "卧推", "sets" to "many", "weightKg" to "heavy"))
        )

        assertEquals("卧推", suggestion?.actionName)
        assertNull(suggestion?.sets)
        assertNull(suggestion?.weightKg)
    }

    @Test
    fun `omits a blank part name`() {
        val suggestion = DailyFitnessA2uiActions.suggestionOf(
            adoptEvent(mapOf("partName" to "  ", "actionName" to "卧推"))
        )

        assertNull(suggestion?.partName)
    }

    @Test
    fun `requires an action name`() {
        assertNull(
            DailyFitnessA2uiActions.suggestionOf(
                adoptEvent(mapOf("partName" to "胸部", "sets" to 3))
            )
        )
    }

    @Test
    fun `ignores other event types`() {
        assertNull(
            DailyFitnessA2uiActions.suggestionOf(
                A2uiClientEventMessage(
                    type = "openSettings",
                    surfaceId = "plan-1",
                    componentId = "action-1",
                    timestamp = 0L,
                    context = mapOf("actionName" to "卧推"),
                )
            )
        )
    }

    @Test
    fun `ignores non event messages`() {
        assertNull(
            DailyFitnessA2uiActions.suggestionOf(
                A2uiClientErrorMessage(code = "RUNTIME_ERROR", surfaceId = "plan-1", message = "boom")
            )
        )
    }

    private fun adoptEvent(context: Map<String, Any?>): A2uiClientEventMessage =
        A2uiClientEventMessage(
            type = A2uiActionContract.ADOPT_EVENT,
            surfaceId = "plan-1",
            componentId = "action-1",
            timestamp = 0L,
            context = context,
        )
}
