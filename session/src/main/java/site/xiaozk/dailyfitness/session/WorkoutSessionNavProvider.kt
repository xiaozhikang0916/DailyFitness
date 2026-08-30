package site.xiaozk.dailyfitness.session

import android.app.PendingIntent

/**
 * Navigation capability required by the session notification.
 *
 * Defined here so the [:session] module never depends on the app's NavKeys /
 * MainActivity. The app provides the real implementation (wiring the pending
 * intents to its activity and Navigation3 back stack); until then a local
 * fallback opens the launcher activity only.
 */
interface WorkoutSessionNavProvider {

    /** PendingIntent for the "add one set" notification action. */
    fun pendingIntentAddSet(): PendingIntent

    /** PendingIntent for tapping the notification body (open today's page). */
    fun pendingIntentOpenToday(): PendingIntent
}
