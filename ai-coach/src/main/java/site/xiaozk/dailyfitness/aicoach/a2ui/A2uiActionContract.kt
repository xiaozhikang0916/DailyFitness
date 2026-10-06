package site.xiaozk.dailyfitness.aicoach.a2ui

/**
 * The action contract the app relies on for agent-authored A2UI surfaces.
 *
 * The agent authors the action payloads, so these names are part of the prompt contract.
 * Keeping them in one place gives the prompt builder (`:ai-coach`), the catalog component
 * (`:ai-coach-ui`) and the outbound-event mapper a single source of truth.
 */
object A2uiActionContract {

    /** Event name dispatched when the user adopts one recommended action row. */
    const val ADOPT_EVENT: String = "adopt"

    /** Context key: body part name of the adopted action. */
    const val KEY_PART_NAME: String = "partName"

    /** Context key: action name of the adopted action. */
    const val KEY_ACTION_NAME: String = "actionName"

    /** Context key: recommended number of sets. */
    const val KEY_SETS: String = "sets"

    /** Context key: recommended reps per set. */
    const val KEY_REPS: String = "reps"

    /** Context key: recommended weight in kg. */
    const val KEY_WEIGHT_KG: String = "weightKg"

    /** Context key: recommended duration in seconds. */
    const val KEY_DURATION_SEC: String = "durationSec"
}
