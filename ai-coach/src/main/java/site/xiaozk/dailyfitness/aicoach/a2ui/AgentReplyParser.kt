package site.xiaozk.dailyfitness.aicoach.a2ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * One parsed reply from the agent-driven UI flow.
 *
 * A2UI itself has no notion of "I need more data", so the agent answers in a two-variant
 * envelope: either the control variant ([NeedMore]) or A2UI protocol messages ([Ui]).
 */
sealed interface AgentReply {

    /**
     * The agent judged the supplied training history insufficient and wants more.
     * [wantSessions] is the requested number of additional complete training days.
     */
    data class NeedMore(val wantSessions: Int?) : AgentReply

    /**
     * The agent produced an A2UI surface. [messages] are the raw protocol messages
     * (one JSON object each) to feed into the A2UI message processor; [surfaceId] is
     * the id the agent used in `createSurface`, used to associate the rendered surface
     * with the conversation turn.
     */
    data class Ui(val surfaceId: String, val messages: List<String>) : AgentReply
}

/** Raised when the agent reply does not follow the JSON Lines envelope contract. */
class AgentReplyParseException(message: String) : IllegalStateException(message)

/**
 * Parses the agent's JSON Lines reply into an [AgentReply].
 *
 * Tolerant by design: markdown fences and surrounding prose are ignored, and messages may
 * arrive either as one JSON object per line or as a single JSON array. The variant is
 * decided by the **first** JSON value: an object carrying `"control":"needMore"` is the
 * control reply, anything else is treated as A2UI protocol messages.
 */
object AgentReplyParser {

    private const val FIELD_CONTROL = "control"
    private const val CONTROL_NEED_MORE = "needMore"
    private const val FIELD_WANT_SESSIONS = "wantSessions"
    private const val FIELD_CREATE_SURFACE = "createSurface"
    private const val FIELD_SURFACE_ID = "surfaceId"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(raw: String): Result<AgentReply> {
        val values = extractJsonValues(stripCodeFences(raw))
        if (values.isEmpty()) {
            return failure("the agent reply contained no JSON")
        }

        val first = runCatching { json.parseToJsonElement(values.first()) }.getOrNull()
            ?: return failure("the agent reply is not valid JSON")

        (first as? JsonObject)?.let { envelope ->
            if (envelope[FIELD_CONTROL]?.jsonPrimitive?.contentOrNull == CONTROL_NEED_MORE) {
                return Result.success(AgentReply.NeedMore(wantSessionsOf(envelope[FIELD_WANT_SESSIONS])))
            }
        }

        val messages = values.flatMap(::messagesOf)
        if (messages.isEmpty()) {
            return failure("the agent reply contained no A2UI messages")
        }
        val surfaceId = messages.firstNotNullOfOrNull(::surfaceIdOf)
            ?: return failure("the agent reply did not create an A2UI surface")
        return Result.success(AgentReply.Ui(surfaceId, messages))
    }

    /** A single JSON array is expanded into one message per element. */
    private fun messagesOf(value: String): List<String> =
        when (val element = runCatching { json.parseToJsonElement(value) }.getOrNull()) {
            is JsonArray -> element.map { it.toString() }
            null -> emptyList()
            else -> listOf(value)
        }

    private fun surfaceIdOf(message: String): String? {
        val envelope =
            runCatching { json.parseToJsonElement(message) }.getOrNull() as? JsonObject ?: return null
        val createSurface = envelope[FIELD_CREATE_SURFACE] as? JsonObject ?: return null
        return createSurface[FIELD_SURFACE_ID]?.jsonPrimitive?.contentOrNull
    }

    private fun wantSessionsOf(element: JsonElement?): Int? {
        val primitive = element?.jsonPrimitive ?: return null
        return primitive.intOrNull ?: primitive.contentOrNull?.trim()?.toIntOrNull()
    }

    private fun failure(message: String): Result<AgentReply> =
        Result.failure(AgentReplyParseException(message))

    /** Drops markdown fence lines; the model often wraps JSON in ```json blocks. */
    private fun stripCodeFences(raw: String): String =
        raw.lineSequence().filterNot { it.trimStart().startsWith("```") }.joinToString("\n")

    /**
     * Extracts every top-level JSON object/array from [text], ignoring anything in between
     * (prose, fences, separators). Handles pretty-printed multi-line values because it
     * scans for balanced brackets instead of splitting on lines.
     */
    private fun extractJsonValues(text: String): List<String> {
        val values = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (char == '{' || char == '[') {
                val end = matchingBracket(text, index) ?: break
                values += text.substring(index, end + 1)
                index = end + 1
            } else {
                index++
            }
        }
        return values
    }

    /** Index of the bracket closing the one at [start], ignoring brackets inside strings. */
    private fun matchingBracket(text: String, start: Int): Int? {
        val open = text[start]
        val close = if (open == '{') '}' else ']'
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until text.length) {
            val char = text[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
            } else {
                when (char) {
                    '"' -> inString = true
                    open -> depth++
                    close -> {
                        depth--
                        if (depth == 0) return index
                    }
                }
            }
        }
        return null
    }
}
