package site.xiaozk.dailyfitness.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.datetime.LocalDate
import site.xiaozk.dailyfitness.session.SessionIntents

/**
 * Pure mapping from [SessionIntents] action constants (delivered by the
 * notification via [NavIntentBus]) to the corresponding [NavKey].
 *
 * Extracted from AppHost so the contract can be unit tested without Compose.
 */
internal fun sessionActionToNavKey(action: String?, today: LocalDate): NavKey? = when (action) {
    SessionIntents.ACTION_ADD_SET -> AddWorkoutAction
    SessionIntents.ACTION_OPEN_TODAY -> TrainDay(date = today)
    else -> null
}
