package site.xiaozk.dailyfitness.aicoach.ui.a2ui

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatNumberFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
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
 * Two groups are deliberately **not** registered:
 * - Media components (`Image` / `Video` / `AudioPlayer`): they would require image/video
 *   loading libraries and network access, which conflicts with the AI Coach privacy
 *   invariant (no network unless the user explicitly asks for a recommendation).
 * - `Card`: the chat surface already draws the outer card for every agent turn, so a
 *   card the agent could emit would only produce card-inside-card chrome. Content is
 *   grouped with `Column` / `Row` / `Divider` / `PartCard` instead.
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
            MaterialA2uiBasicCatalogV1Defaults.column,
            MaterialA2uiBasicCatalogV1Defaults.row,
            MaterialA2uiBasicCatalogV1Defaults.list,
            MaterialA2uiBasicCatalogV1Defaults.divider,
            MaterialA2uiBasicCatalogV1Defaults.button,
            // --- DailyFitness-specific components ---
            PartCardComponent,
            ActionRowComponent,
        ),
        functions = dailyFitnessFunctions(),
        themeSchema = A2uiBasicCatalogV1.ThemeSchema,
        // The catalog is app-private, so its full schema is advertised inline to the agent.
        isInline = true,
    )
}

/**
 * The client-side functions this catalog exposes.
 *
 * The full Basic Catalog set is deliberately trimmed: every function is serialized into the
 * inline schema and re-sent on every request, and the dropped ones are either unused or
 * contrary to the app's invariants.
 *
 * - kept: `formatString` (compose text from data-model values) and `formatNumber`.
 * - dropped: `openUrl` (the app never opens agent-supplied links), `pluralize` /
 *   `formatCurrency` / `formatDate` (this UI formats volumes itself), and the validation
 *   functions (`and` / `or` / `not` / `numeric` / `length` / `regex` / `email` / `required`),
 *   which only serve `checks` on form components this catalog does not register.
 */
private fun dailyFitnessFunctions(): List<A2uiFunction> = listOf(
    A2uiFormatStringFunction.INSTANCE,
    A2uiFormatNumberFunction(A2uiLocaleProvider.Default),
)
