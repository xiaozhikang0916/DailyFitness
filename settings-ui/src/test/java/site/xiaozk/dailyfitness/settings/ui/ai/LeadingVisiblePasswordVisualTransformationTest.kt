package site.xiaozk.dailyfitness.settings.ui.ai

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class LeadingVisiblePasswordVisualTransformationTest {

    private val transformation = LeadingVisiblePasswordVisualTransformation()

    @Test
    fun `keeps first three characters and masks the rest`() {
        val transformed = transformation.filter(AnnotatedString("sk-abcdef123456")).text.text
        assertEquals("sk-" + "\u2022".repeat(12), transformed)
    }

    @Test
    fun `short secrets are fully visible`() {
        assertEquals("ab", transformation.filter(AnnotatedString("ab")).text.text)
    }

    @Test
    fun `empty input stays empty`() {
        assertEquals("", transformation.filter(AnnotatedString("")).text.text)
    }

    @Test
    fun `offset mapping is identity because length is preserved`() {
        val source = AnnotatedString("sk-abcdef123456")
        val offsetMapping = transformation.filter(source).offsetMapping
        for (offset in 0..source.text.length) {
            assertEquals(offset, offsetMapping.originalToTransformed(offset))
            assertEquals(offset, offsetMapping.transformedToOriginal(offset))
        }
    }

    @Test
    fun `custom visible count and mask are honoured`() {
        val custom = LeadingVisiblePasswordVisualTransformation(visibleCount = 1, mask = '*')
        assertEquals("s" + "*".repeat(14), custom.filter(AnnotatedString("sk-abcdef123456")).text.text)
    }
}
