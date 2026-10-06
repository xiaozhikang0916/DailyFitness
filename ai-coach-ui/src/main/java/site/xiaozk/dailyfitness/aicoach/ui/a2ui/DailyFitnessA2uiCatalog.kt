package site.xiaozk.dailyfitness.aicoach.ui.a2ui

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.catalog.basiccatalog.createBasicCatalogFunctions
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.catalog.functions.A2uiMessageFormatter
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import site.xiaozk.dailyfitness.aicoach.ui.a2ui.components.ActionRowComponent
import site.xiaozk.dailyfitness.aicoach.ui.a2ui.components.PartCardComponent

/**
 * The AI Coach A2UI component catalog (hybrid).
 *
 * Most components are reused straight from the Material 3 Basic Catalog
 * ([MaterialA2uiBasicCatalogV1Defaults]); the DailyFitness-specific ones
 * ([PartCardComponent], [ActionRowComponent]) are appended by this app.
 *
 * Media components (`Image` / `Video` / `AudioPlayer`) are deliberately **not**
 * registered: they would require image/video loading libraries and network access,
 * which conflicts with the AI Coach privacy invariant (no network unless the user
 * explicitly asks for a recommendation) and with "no new heavy dependencies".
 */
object DailyFitnessA2uiCatalog {

    /**
     * The catalog identifier advertised to the agent during capability negotiation.
     * The agent must copy it verbatim into every `createSurface` message.
     */
    const val ID: String = "https://dailyfitness.xiaozk.site/a2ui/v1/catalog.json"

    /** The single catalog instance shared by the processor, the renderer and the prompt. */
    val catalog: A2uiCatalog = A2uiCatalog(
        catalogId = ID,
        components = listOf(
            // --- Reused Material 3 Basic Catalog components ---
            MaterialA2uiBasicCatalogV1Defaults.text,
            MaterialA2uiBasicCatalogV1Defaults.icon,
            MaterialA2uiBasicCatalogV1Defaults.card,
            MaterialA2uiBasicCatalogV1Defaults.column,
            MaterialA2uiBasicCatalogV1Defaults.row,
            MaterialA2uiBasicCatalogV1Defaults.list,
            MaterialA2uiBasicCatalogV1Defaults.divider,
            MaterialA2uiBasicCatalogV1Defaults.button,
            // --- DailyFitness-specific components ---
            PartCardComponent,
            ActionRowComponent,
        ),
        functions = createBasicCatalogFunctions(
            // Privacy invariant: external links are never opened from agent-generated UI.
            urlOpener = { },
            messageFormatter = A2uiMessageFormatter { pattern, locale, arguments ->
                android.icu.text.MessageFormat(pattern, locale).format(arguments)
            },
            localeProvider = A2uiLocaleProvider.Default,
        ),
        themeSchema = A2uiBasicCatalogV1.ThemeSchema,
        // The catalog is app-private, so its full schema is advertised inline to the agent.
        isInline = true,
    )
}
