package site.xiaozk.dailyfitness.settings.ui.ai

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Like [androidx.compose.ui.text.input.PasswordVisualTransformation], but keeps the first
 * [visibleCount] characters readable so the user can confirm which key they pasted, while
 * masking every following character with [mask].
 *
 * The transformed text keeps the same length as the source, so [OffsetMapping.Identity]
 * is used (cursor offsets remain valid).
 */
@Immutable
class LeadingVisiblePasswordVisualTransformation(
    private val visibleCount: Int = DEFAULT_VISIBLE_COUNT,
    val mask: Char = '\u2022',
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = buildAnnotatedString {
                text.text.forEachIndexed { index, char ->
                    append(if (index < visibleCount) char else mask)
                }
            },
            offsetMapping = OffsetMapping.Identity,
        )
    }

    companion object {
        const val DEFAULT_VISIBLE_COUNT = 3
    }
}
