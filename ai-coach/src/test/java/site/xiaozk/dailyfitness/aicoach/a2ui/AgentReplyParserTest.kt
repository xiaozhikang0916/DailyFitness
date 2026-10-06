package site.xiaozk.dailyfitness.aicoach.a2ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Offline tests of the agent reply envelope: A2UI itself has no "need more data" message, so
 * the agent answers in a two-variant JSON Lines envelope that this parser has to recognise.
 */
class AgentReplyParserTest {

    @Test
    fun `parses the needMore control envelope`() {
        val reply = AgentReplyParser.parse("""{"control":"needMore","wantSessions":3}""").getOrThrow()

        assertEquals(AgentReply.NeedMore(3), reply)
    }

    @Test
    fun `accepts wantSessions as a numeric string`() {
        val reply = AgentReplyParser.parse("""{"control":"needMore","wantSessions":"4"}""").getOrThrow()

        assertEquals(AgentReply.NeedMore(4), reply)
    }

    @Test
    fun `treats a missing wantSessions as unspecified`() {
        val reply = AgentReplyParser.parse("""{"control":"needMore"}""").getOrThrow()

        assertEquals(AgentReply.NeedMore(null), reply)
    }

    @Test
    fun `parses a JSON Lines A2UI exchange and extracts the surface id`() {
        val reply = AgentReplyParser.parse(UI_REPLY).getOrThrow() as AgentReply.Ui

        assertEquals("plan-1", reply.surfaceId)
        assertEquals(2, reply.messages.size)
        assertTrue(reply.messages.first().contains("createSurface"))
    }

    @Test
    fun `tolerates markdown fences and surrounding prose`() {
        val raw = """
            Sure, here is the plan:
            ```json
            {"version":"v0.9","createSurface":{"surfaceId":"plan-2","catalogId":"$CATALOG_ID"}}
            {"version":"v0.9","updateComponents":{"surfaceId":"plan-2","components":[{"id":"root","component":"Text"}]}}
            ```
            Let me know if you want changes.
        """.trimIndent()

        val reply = AgentReplyParser.parse(raw).getOrThrow() as AgentReply.Ui

        assertEquals("plan-2", reply.surfaceId)
        assertEquals(2, reply.messages.size)
    }

    @Test
    fun `expands a single JSON array into individual messages`() {
        val raw =
            """[{"version":"v0.9","createSurface":{"surfaceId":"plan-3","catalogId":"$CATALOG_ID"}},{"version":"v0.9","updateComponents":{"surfaceId":"plan-3","components":[]}}]"""

        val reply = AgentReplyParser.parse(raw).getOrThrow() as AgentReply.Ui

        assertEquals("plan-3", reply.surfaceId)
        assertEquals(2, reply.messages.size)
    }

    @Test
    fun `survives pretty printed multi line messages`() {
        val raw = """
            {
              "version": "v0.9",
              "createSurface": {
                "surfaceId": "plan-4",
                "catalogId": "$CATALOG_ID"
              }
            }
            {
              "version": "v0.9",
              "updateComponents": {
                "surfaceId": "plan-4",
                "components": [{"id": "root", "component": "Text", "text": "brace } inside a string"}]
              }
            }
        """.trimIndent()

        val reply = AgentReplyParser.parse(raw).getOrThrow() as AgentReply.Ui

        assertEquals("plan-4", reply.surfaceId)
        assertEquals(2, reply.messages.size)
    }

    @Test
    fun `fails when the reply contains no JSON`() {
        val error = AgentReplyParser.parse("I cannot help with that.").exceptionOrNull()

        assertTrue(error is AgentReplyParseException)
    }

    @Test
    fun `fails when the reply never creates a surface`() {
        val error = AgentReplyParser
            .parse("""{"version":"v0.9","updateComponents":{"surfaceId":"plan-5","components":[]}}""")
            .exceptionOrNull()

        assertTrue(error is AgentReplyParseException)
    }

    private companion object {
        const val CATALOG_ID = "https://dailyfitness.xiaozk.site/a2ui/v1/catalog.json"

        val UI_REPLY = """
            {"version":"v0.9","createSurface":{"surfaceId":"plan-1","catalogId":"$CATALOG_ID","sendDataModel":false}}
            {"version":"v0.9","updateComponents":{"surfaceId":"plan-1","components":[{"id":"root","component":"PartCard","partName":"Chest","children":[]}]}}
        """.trimIndent()
    }
}
