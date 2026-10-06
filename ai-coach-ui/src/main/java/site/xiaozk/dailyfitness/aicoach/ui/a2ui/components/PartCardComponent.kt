package site.xiaozk.dailyfitness.aicoach.ui.a2ui.components

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.observeA2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import site.xiaozk.dailyfitness.aicoach.ui.R

/**
 * A2UI custom component rendering one recommended part (title, primary/backup
 * badge, reason and its action lines), mirroring the legacy `RecommendedPartCard`.
 *
 * Children are resolved as A2UI component references ([ActionRowComponent] rows),
 * so the agent composes the card by referencing child ids instead of inlining.
 */
object PartCardComponent : A2uiComponent {

    override val name: String = "PartCard"

    override val description: String =
        "A recommended body part for today: its name, whether it is the primary " +
            "recommendation, an optional reason and its list of action rows."

    val PartNameProperty = A2uiProperty.dynamicString(
        key = "partName",
        required = true,
        description = "The body part name. Must be copied verbatim from the exercise catalog.",
    )

    val IsPrimaryProperty = A2uiProperty.dynamicBoolean(
        key = "isPrimary",
        description = "Whether this is the primary recommendation for today.",
    )

    val ReasonProperty = A2uiProperty.dynamicString(
        key = "reason",
        description = "Short reason for recommending this part.",
    )

    val ChildrenProperty = A2uiProperty.childList(
        key = "children",
        required = true,
        description =
            "The action rows of this part, referred to by component id. Use ActionRow components.",
    )

    override val properties: List<A2uiProperty<*>> = listOf(
        A2uiBasicCatalogV1.WeightProperty,
        PartNameProperty,
        IsPrimaryProperty,
        ReasonProperty,
        ChildrenProperty,
    )

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
        properties.bind(PartNameProperty) != null &&
            properties.bindChildReferences(ChildrenProperty) != null

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val partName = properties.bind(PartNameProperty) ?: return
        val isPrimary = properties.bind(IsPrimaryProperty) ?: false
        val reason = properties.bind(ReasonProperty)
        val children = properties.bindChildReferences(ChildrenProperty).orEmpty()

        Column(modifier = modifier) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(
                    text = partName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (isPrimary) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ai_part_primary),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            reason?.let {
                Text(
                    text = stringResource(R.string.ai_reason_label, it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            children.forEach { reference ->
                key(reference.id, reference.baseDataPath) {
                    when (val childState = observeA2uiComponentState(reference)) {
                        is A2uiComponentState.Loading ->
                            CircularProgressIndicator(
                                modifier = ChildLoadingModifier,
                                strokeWidth = 2.dp,
                            )
                        // Child errors are already reported back to the agent by the A2UI
                        // runtime, so nothing extra is drawn here.
                        is A2uiComponentState.Error -> Unit
                        is A2uiComponentState.Success ->
                            A2uiComponent(component = childState.component)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        }
    }
}

private val ChildLoadingModifier = Modifier.size(20.dp)
