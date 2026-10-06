package site.xiaozk.dailyfitness.aicoach.ui.a2ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.a2ui.A2uiActionContract

/**
 * The dispatched payload must always match what the row renders, while still carrying the
 * agent's event name and the part name it alone knows.
 */
class ActionRowComponentTest {

    @Test
    fun `component values win over the agent context`() {
        val payload = adoptPayload(
            action = eventAction(
                context = mapOf(
                    A2uiActionContract.KEY_PART_NAME to "胸部",
                    // The agent's numbers are stale; the rendered ones must win.
                    A2uiActionContract.KEY_SETS to 99,
                    A2uiActionContract.KEY_REPS to 99,
                )
            ),
            actionName = "卧推",
            sets = 4,
            reps = 8,
            weightKg = 62.5,
            durationSec = null,
        )

        val context = contextOf(payload)
        assertEquals("胸部", context[A2uiActionContract.KEY_PART_NAME])
        assertEquals("卧推", context[A2uiActionContract.KEY_ACTION_NAME])
        assertEquals(4, context[A2uiActionContract.KEY_SETS])
        assertEquals(8, context[A2uiActionContract.KEY_REPS])
        assertEquals(62.5, context[A2uiActionContract.KEY_WEIGHT_KG])
    }

    @Test
    fun `omits the values the action type does not support`() {
        val payload = adoptPayload(
            action = eventAction(context = mapOf(A2uiActionContract.KEY_PART_NAME to "胸部")),
            actionName = "平板支撑",
            sets = 3,
            reps = null,
            weightKg = null,
            durationSec = 60,
        )

        val context = contextOf(payload)
        assertEquals(3, context[A2uiActionContract.KEY_SETS])
        assertEquals(60, context[A2uiActionContract.KEY_DURATION_SEC])
        assertEquals(false, context.containsKey(A2uiActionContract.KEY_REPS))
        assertEquals(false, context.containsKey(A2uiActionContract.KEY_WEIGHT_KG))
    }

    @Test
    fun `falls back to the adopt event name when the agent omitted the action`() {
        val payload = adoptPayload(
            action = null,
            actionName = "卧推",
            sets = 3,
            reps = null,
            weightKg = null,
            durationSec = null,
        )

        assertEquals(A2uiActionContract.ADOPT_EVENT, eventNameOf(payload))
        assertEquals("卧推", contextOf(payload)[A2uiActionContract.KEY_ACTION_NAME])
    }

    @Test
    fun `keeps a custom event name authored by the agent`() {
        val payload = adoptPayload(
            action = eventAction(name = "adoptNow"),
            actionName = "卧推",
            sets = 3,
            reps = null,
            weightKg = null,
            durationSec = null,
        )

        assertEquals("adoptNow", eventNameOf(payload))
    }

    @Suppress("UNCHECKED_CAST")
    private fun contextOf(payload: Map<String, Any?>): Map<String, Any?> =
        (payload["event"] as Map<String, Any?>)["context"] as Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun eventNameOf(payload: Map<String, Any?>): String? =
        (payload["event"] as Map<String, Any?>)["name"] as String?

    private fun eventAction(
        name: String = A2uiActionContract.ADOPT_EVENT,
        context: Map<String, Any?> = emptyMap(),
    ): Map<String, Any?> = mapOf("event" to mapOf("name" to name, "context" to context))
}
