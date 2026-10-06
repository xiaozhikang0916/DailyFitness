package site.xiaozk.dailyfitness.aicoach.ui.a2ui.components

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import site.xiaozk.dailyfitness.aicoach.a2ui.A2uiActionContract
import site.xiaozk.dailyfitness.aicoach.ui.R

/**
 * A2UI custom component rendering one recommended action line (name + volume + an
 * adopt button), mirroring the legacy `ActionLine` of the AI Coach tab.
 *
 * Free text (`actionName`) is agent/user data; everything the UI formats itself
 * (set/reps/weight/duration labels, the adopt label) resolves through string
 * resources, so no display copy is hardcoded in the domain layer.
 */
object ActionRowComponent : A2uiComponent {

    override val name: String = "ActionRow"

    override val description: String =
        "One recommended exercise line: the action name, its recommended volume " +
            "and an \"adopt\" button that carries this action back to the agent."

    val ActionNameProperty = A2uiProperty.dynamicString(
        key = "actionName",
        required = true,
        description = "The action name. Must be copied verbatim from the exercise catalog.",
    )

    val SetsProperty = A2uiProperty.dynamicNumber(
        key = "sets",
        description = "Recommended number of sets.",
    )

    val RepsProperty = A2uiProperty.dynamicNumber(
        key = "reps",
        description = "Recommended reps per set (counted actions only).",
    )

    val WeightProperty = A2uiProperty.dynamicNumber(
        key = "weightKg",
        description = "Recommended weight in kg (weighted actions only).",
    )

    val DurationProperty = A2uiProperty.dynamicNumber(
        key = "durationSec",
        description = "Recommended duration in seconds (timed actions only).",
    )

    val ActionProperty = A2uiProperty.action(
        key = "action",
        required = true,
        description =
            "The action dispatched when the user adopts this line. Its context must carry " +
                "partName/actionName/sets/reps/weightKg/durationSec so the host can prefill a set.",
    )

    override val properties: List<A2uiProperty<*>> = listOf(
        A2uiBasicCatalogV1.WeightProperty,
        ActionNameProperty,
        SetsProperty,
        RepsProperty,
        WeightProperty,
        DurationProperty,
        ActionProperty,
    )

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
        properties.bind(ActionNameProperty) != null

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val actionName = properties.bind(ActionNameProperty) ?: return
        val sets = properties.bind(SetsProperty)?.toInt()
        val reps = properties.bind(RepsProperty)?.toInt()
        val weightKg = properties.bind(WeightProperty)?.toDouble()
        val durationSec = properties.bind(DurationProperty)?.toInt()

        // The component owns the payload numbers: a row always dispatches exactly what it
        // renders, while the agent's action contributes the event name and the part name
        // (the only value this row cannot know).
        val action = properties[ActionProperty]
        val onAdopt: () -> Unit = {
            dispatchAction(
                adoptPayload(
                    action = action,
                    actionName = actionName,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    durationSec = durationSec,
                )
            )
        }

        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = actionLineText(actionName, sets, reps, weightKg, durationSec),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            TextButton(onClick = onAdopt) {
                Text(stringResource(R.string.ai_adopt))
            }
        }
    }
}

/**
 * Builds the outbound action payload for [A2uiActionContract.ADOPT_EVENT].
 *
 * The agent's action payload supplies the event name and the part name; the action's own
 * numbers are taken from the values this row resolves, so the prefilled set can never
 * disagree with what the user sees.
 */
internal fun adoptPayload(
    action: Map<String, Any?>?,
    actionName: String,
    sets: Int?,
    reps: Int?,
    weightKg: Double?,
    durationSec: Int?,
): Map<String, Any?> {
    val event = action?.get(EVENT_KEY) as? Map<*, *>
    val eventName = (event?.get(EVENT_NAME_KEY) as? String)?.takeIf { it.isNotBlank() }
        ?: A2uiActionContract.ADOPT_EVENT
    @Suppress("UNCHECKED_CAST")
    val agentContext = event?.get(EVENT_CONTEXT_KEY) as? Map<String, Any?> ?: emptyMap()

    val context = agentContext + buildMap {
        put(A2uiActionContract.KEY_ACTION_NAME, actionName)
        sets?.let { put(A2uiActionContract.KEY_SETS, it) }
        reps?.let { put(A2uiActionContract.KEY_REPS, it) }
        weightKg?.let { put(A2uiActionContract.KEY_WEIGHT_KG, it) }
        durationSec?.let { put(A2uiActionContract.KEY_DURATION_SEC, it) }
    }
    return mapOf(EVENT_KEY to mapOf(EVENT_NAME_KEY to eventName, EVENT_CONTEXT_KEY to context))
}

private const val EVENT_KEY = "event"
private const val EVENT_NAME_KEY = "name"
private const val EVENT_CONTEXT_KEY = "context"

/**
 * Formats one action line the same way the legacy chat bubble did, so the A2UI
 * rendering stays consistent with the rest of the tab.
 */
@Composable
private fun actionLineText(
    actionName: String,
    sets: Int?,
    reps: Int?,
    weightKg: Double?,
    durationSec: Int?,
): String {
    val params = buildList {
        weightKg?.let { add(stringResource(R.string.ai_action_weight, formatNumber(it))) }
        reps?.let { add(stringResource(R.string.ai_action_reps, it)) }
        durationSec?.let { add(stringResource(R.string.ai_action_duration, it)) }
    }.joinToString(" ")
    val head = if (sets != null) {
        actionName + " · " + stringResource(R.string.ai_action_sets, sets)
    } else {
        actionName
    }
    return listOf(head, params).filter { it.isNotBlank() }.joinToString(" ")
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
