package site.xiaozk.dailyfitness.aicoach.ui.a2ui

import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import site.xiaozk.dailyfitness.aicoach.a2ui.A2uiActionContract
import site.xiaozk.dailyfitness.aicoach.engine.CoachSuggestion

/**
 * Translates the agent-authored surface's outbound actions into the app's own vocabulary.
 *
 * A surface action is a one-time user intent (A2UI [A2uiClientToServerMessage]); the app needs
 * a [CoachSuggestion] to prefill the "add a set" page. This is the only place that knows both
 * sides.
 */
object DailyFitnessA2uiActions {

    /**
     * Maps an outbound A2UI message to an adopt request, or `null` when the message is not an
     * adopt action (or carries no usable action name).
     */
    fun suggestionOf(message: A2uiClientToServerMessage): CoachSuggestion? {
        val event = message as? A2uiClientEventMessage ?: return null
        if (event.type != A2uiActionContract.ADOPT_EVENT) return null

        val context = event.context
        val actionName = context[A2uiActionContract.KEY_ACTION_NAME]
            .asText()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return CoachSuggestion(
            partName = context[A2uiActionContract.KEY_PART_NAME].asText()?.takeIf { it.isNotBlank() },
            actionName = actionName,
            sets = context[A2uiActionContract.KEY_SETS].asInt(),
            reps = context[A2uiActionContract.KEY_REPS].asInt(),
            weightKg = context[A2uiActionContract.KEY_WEIGHT_KG].asDouble(),
            durationSec = context[A2uiActionContract.KEY_DURATION_SEC].asInt(),
        )
    }
}

private fun Any?.asText(): String? = when (this) {
    null -> null
    is String -> this
    else -> toString()
}

private fun Any?.asInt(): Int? = when (this) {
    null -> null
    is Number -> toInt()
    is String -> trim().toIntOrNull()
    else -> null
}

private fun Any?.asDouble(): Double? = when (this) {
    null -> null
    is Number -> toDouble()
    is String -> trim().toDoubleOrNull()
    else -> null
}
