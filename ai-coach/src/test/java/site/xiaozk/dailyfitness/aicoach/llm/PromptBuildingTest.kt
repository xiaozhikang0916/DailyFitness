package site.xiaozk.dailyfitness.aicoach.llm

import ai.koog.prompt.message.Message
import org.junit.Assert.assertEquals
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage

/**
 * Verifies the exact LLM request built for the AI Coach: a system rule message,
 * the previous conversation as alternating user/assistant messages (newest last),
 * and the fresh user turn. Pure construction - no network involved.
 */
class PromptBuildingTest {

    @Test
    fun `prompt contains system, previous turns and the current user turn in order`() {
        val history = listOf(
            CoachMessage(fromUser = true, text = "历史用户1"),
            CoachMessage(fromUser = false, text = "历史建议1"),
            CoachMessage(fromUser = true, text = "历史用户2"),
            CoachMessage(fromUser = false, text = "历史建议2"),
        )

        val prompt = buildAiCoachPrompt(
            promptId = "aicoach-part-plan",
            systemText = "SYSTEM-RULES",
            userText = "CURRENT-DATA",
            history = history,
        )

        assertEquals(
            listOf(
                Message.Role.System,
                Message.Role.User,
                Message.Role.Assistant,
                Message.Role.User,
                Message.Role.Assistant,
                Message.Role.User,
            ),
            prompt.messages.map { it.role },
        )
        assertEquals(
            listOf("SYSTEM-RULES", "历史用户1", "历史建议1", "历史用户2", "历史建议2", "CURRENT-DATA"),
            prompt.messages.map { it.textContent() },
        )
    }

    @Test
    fun `prompt with empty history is system plus current user only`() {
        val prompt = buildAiCoachPrompt(
            promptId = "aicoach-next-advice",
            systemText = "SYSTEM-RULES",
            userText = "CURRENT-DATA",
            history = emptyList(),
        )

        assertEquals(
            listOf(Message.Role.System, Message.Role.User),
            prompt.messages.map { it.role },
        )
        assertEquals(
            listOf("SYSTEM-RULES", "CURRENT-DATA"),
            prompt.messages.map { it.textContent() },
        )
    }

    @Test
    fun `assistant-only history keeps its role`() {
        val prompt = buildAiCoachPrompt(
            promptId = "aicoach-next-advice",
            systemText = "S",
            userText = "U",
            history = listOf(CoachMessage(fromUser = false, text = "仅建议")),
        )

        assertEquals(
            listOf(Message.Role.System, Message.Role.Assistant, Message.Role.User),
            prompt.messages.map { it.role },
        )
    }
}
