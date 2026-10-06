package site.xiaozk.dailyfitness.aicoach.a2ui

/**
 * Supplies the A2UI catalog capability this app advertises to the agent during
 * capability negotiation.
 *
 * The catalog itself is a Compose construct and therefore lives in `:ai-coach-ui`;
 * this interface is the seam that lets the domain layer (prompt building) read the
 * catalog id and its JSON schema without depending on Compose. The host module binds
 * the implementation.
 */
interface A2uiCapabilityProvider {

    /**
     * The catalog id the agent must reference verbatim in every `createSurface`
     * message (for example `https://dailyfitness.xiaozk.site/a2ui/v1/catalog.json`).
     */
    val catalogId: String

    /**
     * The catalog's JSON schema. Embedded into the system prompt as an inline catalog
     * so the agent knows exactly which components and properties it may emit.
     */
    fun inlineCatalogJson(): String
}
