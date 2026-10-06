package site.xiaozk.dailyfitness.aicoach.ui.a2ui

import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.compose.ui.toJsonSchemaMap
import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiCreateSurfaceMessage
import androidx.a2ui.model.protocol.A2uiUpdateComponentsMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the Phase A A2UI foundation without any LLM:
 * - the hybrid catalog is well-formed and serializes to a schema,
 * - media components are not advertised (privacy invariant),
 * - the message processor resolves an agent-authored surface using the custom components.
 *
 * Protocol messages are injected at the [androidx.a2ui.model.processor.A2uiMessageProcessor]
 * level rather than as raw JSON: the JSON reader is backed by `android.util.JsonReader`, which is
 * unavailable in local JVM unit tests. The raw-JSON envelope path is covered by the instrumented
 * tests planned in Phase D.
 */
class DailyFitnessA2uiCatalogTest {

    @Test
    fun `catalog schema advertises reused and custom components`() {
        val json = DailyFitnessA2uiCatalog.catalog.toJsonSchemaString()

        // Custom DailyFitness components.
        assertTrue("PartCard missing from schema", json.contains("\"PartCard\""))
        assertTrue("ActionRow missing from schema", json.contains("\"ActionRow\""))
        // Representative reused Material 3 basic components (the ones that group content,
        // since the app owns the outer card).
        assertTrue("Text missing from schema", json.contains("\"Text\""))
        assertTrue("Column missing from schema", json.contains("\"Column\""))
        assertTrue("Divider missing from schema", json.contains("\"Divider\""))
    }

    @Test
    fun `catalog does not advertise a card so the app owns the only card chrome`() {
        val json = DailyFitnessA2uiCatalog.catalog.toJsonSchemaString()

        // Quoted, so the custom "PartCard" does not count as a hit.
        assertFalse("Card must not be advertised", json.contains("\"Card\""))
    }

    @Test
    fun `catalog does not advertise media components`() {
        val map = DailyFitnessA2uiCatalog.catalog.toJsonSchemaMap().toString()

        assertFalse("Image must not be advertised", map.contains("Image"))
        assertFalse("Video must not be advertised", map.contains("Video"))
        assertFalse("AudioPlayer must not be advertised", map.contains("AudioPlayer"))
    }

    @Test
    fun `catalog id matches the advertised capability id`() {
        assertEquals(
            "https://dailyfitness.xiaozk.site/a2ui/v1/catalog.json",
            DailyFitnessA2uiCatalog.catalog.id,
        )
        assertEquals(DailyFitnessA2uiCatalog.ID, DailyFitnessA2uiCatalog.catalog.id)
    }

    @Test
    fun `catalog exposes only the functions the app actually serves`() {
        val json = DailyFitnessA2uiCatalog.catalog.toJsonSchemaString()

        // Kept: text interpolation and number formatting.
        assertTrue("formatString missing", json.contains("\"formatString\""))
        assertTrue("formatNumber missing", json.contains("\"formatNumber\""))

        // Dropped: link opening (privacy), formatting this UI does itself, and the
        // validation functions that only serve `checks` on form components we do not
        // register. Quoted matches, so function names mentioned inside another
        // function's description do not count as hits.
        listOf("openUrl", "pluralize", "formatCurrency", "formatDate", "email", "regex", "numeric")
            .forEach { name ->
                assertFalse("$name must not be advertised", json.contains("\"$name\""))
            }
    }

    @Test
    fun `processor resolves a PartCard surface authored with custom components`() = runBlocking {
        val processor = A2uiMessageProcessor(catalogs = listOf(DailyFitnessA2uiCatalog.catalog))
        // Drives the processor the same way the ViewModel does: the inbound queue is consumed
        // on a background scope for the lifetime of the processor.
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope.launch { processor.collectMessages() }
        try {
            processor.processMessage(
                A2uiCreateSurfaceMessage(
                    surfaceId = SURFACE_ID,
                    catalogId = DailyFitnessA2uiCatalog.ID,
                    theme = emptyMap(),
                    shouldSendDataModel = false,
                )
            )
            processor.processMessage(
                A2uiUpdateComponentsMessage(
                    surfaceId = SURFACE_ID,
                    components = listOf(
                        A2uiComponentPayload(
                            id = "root",
                            type = "PartCard",
                            properties = mapOf(
                                "partName" to "Chest",
                                "isPrimary" to true,
                                "reason" to "Push session",
                                "children" to listOf(ACTION_ID),
                            ),
                        ),
                        A2uiComponentPayload(
                            id = ACTION_ID,
                            type = "ActionRow",
                            properties = mapOf(
                                "actionName" to "Bench Press",
                                "sets" to 3,
                                "reps" to 10,
                                "weightKg" to 60,
                                "action" to mapOf(
                                    "event" to mapOf(
                                        "name" to "adopt",
                                        "context" to emptyMap<String, Any?>(),
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
            )

            val surfaces = withTimeout(5_000) {
                processor.activeSurfaces.first { it.isNotEmpty() }
            }
            assertEquals(1, surfaces.size)
            assertEquals(SURFACE_ID, surfaces.first().id)
        } finally {
            scope.cancel()
        }
    }

    private companion object {
        const val SURFACE_ID = "plan-1"
        const val ACTION_ID = "action-1"
    }
}
