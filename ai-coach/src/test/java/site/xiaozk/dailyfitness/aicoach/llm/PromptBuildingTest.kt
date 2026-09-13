package site.xiaozk.dailyfitness.aicoach.llm

import ai.koog.prompt.message.Message
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessage
import site.xiaozk.dailyfitness.aicoach.engine.CoachMessageContent
import site.xiaozk.dailyfitness.aicoach.prompt.toPromptText

/**
 * Verifies the exact LLM request built for the AI Coach: a system rule message,
 * the previous conversation as alternating user/assistant messages (newest last),
 * and the fresh user turn. Pure construction - no network involved.
 *
 * The conversation carries UI-agnostic [CoachMessageContent] descriptors; the LLM
 * text is rendered from them via `toPromptText()`.
 */
class PromptBuildingTest {

    private val first = CoachMessageContent.PlanRequest(LocalDate(2025, 1, 1))
    private val second = CoachMessageContent.AdviceRequest(LocalDate(2025, 1, 2), setsToday = 2, partName = "胸部")
    private val third = CoachMessageContent.PlanRequest(LocalDate(2025, 1, 3))
    private val fourth = CoachMessageContent.AdviceRequest(LocalDate(2025, 1, 4), setsToday = 4, partName = "背部")

    @Test
    fun `prompt contains system, previous turns and the current user turn in order`() {
        val history = listOf(
            CoachMessage(fromUser = true, content = first),
            CoachMessage(fromUser = false, content = second),
            CoachMessage(fromUser = true, content = third),
            CoachMessage(fromUser = false, content = fourth),
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
            listOf(
                "SYSTEM-RULES",
                first.toPromptText(),
                second.toPromptText(),
                third.toPromptText(),
                fourth.toPromptText(),
                "CURRENT-DATA",
            ),
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
            history = listOf(CoachMessage(fromUser = false, content = second)),
        )

        assertEquals(
            listOf(Message.Role.System, Message.Role.Assistant, Message.Role.User),
            prompt.messages.map { it.role },
        )
    }
}
