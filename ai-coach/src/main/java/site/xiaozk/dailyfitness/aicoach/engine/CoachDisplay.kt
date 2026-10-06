package site.xiaozk.dailyfitness.aicoach.engine

import kotlinx.datetime.LocalDate

/**
 * Describes the user-visible content of a [CoachMessage] without embedding any
 * display string in this module.
 *
 * The UI layer (:ai-coach-ui) switches on the concrete variant and resolves it to
 * localized string resources, so this module stays free of UI copy.
 */
sealed interface CoachMessageContent {

    /**
     * Case A request: today has no workout records yet.
     *
     * Built locally by the UI layer (today's state is client-side data), so it can be
     * shown immediately without waiting for the LLM round-trip.
     */
    data class PlanRequest(
        val date: LocalDate,
    ) : CoachMessageContent

    /** Case B request: today already has records, asking for the next step. */
    data class AdviceRequest(
        val date: LocalDate,
        val setsToday: Int,
        val partName: String,
    ) : CoachMessageContent

    /** Assistant turn summarizing a recommended plan. */
    data class PlanSummary(
        val parts: List<RecommendedPart>,
        val ignoredNames: List<String> = emptyList(),
    ) : CoachMessageContent

    /** Assistant turn summarizing a next-step advice. */
    data class AdviceSummary(
        val advice: Advice,
        val ignoredNames: List<String> = emptyList(),
    ) : CoachMessageContent

    /**
     * Assistant turn rendered from an agent-authored A2UI surface.
     *
     * [messages] are the raw A2UI protocol messages that build the surface; the UI layer
     * feeds them into the A2UI message processor. [surfaceId] is the id the agent used in
     * `createSurface` and identifies the rendered surface among the processor's active ones.
     */
    data class AgentUi(
        val surfaceId: String,
        val messages: List<String>,
    ) : CoachMessageContent

    /**
     * Transient placeholder appended to the UI conversation while a request is in
     * flight. The UI renders it as a loading bubble and it is never sent to the LLM.
     */
    data object Loading : CoachMessageContent

    /**
     * Terminal assistant turn for a request that failed, timed out or was aborted
     * by the user. Like [Loading] this is a UI-only turn: it belongs to the rendered
     * conversation but must never be sent to the LLM.
     */
    data class Failure(val failure: CoachFailure) : CoachMessageContent
}

/**
 * Describes a failure in a UI-agnostic way; the UI layer resolves it to a
 * localized message.
 *
 * [ModelError.detail] carries raw provider/exception text, i.e. dynamic data
 * rather than UI copy hardcoded in this module.
 */
sealed interface CoachFailure {
    /** API key rejected or missing permission. */
    data object InvalidKey : CoachFailure

    /** Network unreachable. */
    data object Network : CoachFailure

    /** The provider did not answer within the configured request timeout. */
    data object Timeout : CoachFailure

    /** The user cancelled the in-flight request. */
    data object Cancelled : CoachFailure

    /** Provider rate limit (HTTP 429). */
    data object RateLimited : CoachFailure

    /** Any other provider/transport failure, with the raw detail for context. */
    data class ModelError(val detail: String) : CoachFailure

    /** The model asked for more history although none can ever be provided. */
    data object NeedMoreWithoutHistory : CoachFailure

    /** The model kept asking for more history until the expansion rounds ran out. */
    data object InsufficientHistory : CoachFailure

    /** The model returned no usable plan. */
    data object EmptyPlan : CoachFailure

    /** The returned plan referenced parts/actions absent from the library. */
    data object PlanNotMatched : CoachFailure

    /** Today's already-trained parts could not be determined. */
    data object CannotDetermineTodayParts : CoachFailure

    /** The current (most recent) part could not be determined. */
    data object CannotDetermineCurrentPart : CoachFailure

    /** The current part is not present in the action library. */
    data object CurrentPartNotInLibrary : CoachFailure

    /** A switch-action advice named an action absent from the library. */
    data object SuggestedActionNotInLibrary : CoachFailure
}
