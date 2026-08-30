package site.xiaozk.dailyfitness.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import kotlin.time.Instant
import javax.inject.Inject

/**
 * Builds the ongoing session notification and owns the notification channel.
 * Pure presentation: given a [WorkoutSessionState] it produces a [Notification]
 * with the session start time, the current action name, the set count and two
 * actions:
 *  - "add one set" -> [WorkoutSessionNavProvider.pendingIntentAddSet]
 *  - "finish"      -> starts [WorkoutSessionService] with [SessionIntents.ACTION_FINISH]
 */
class WorkoutSessionNotification @Inject constructor(
    @ApplicationContext private val context: Context,
    private val navProvider: WorkoutSessionNavProvider,
) {
    fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun build(state: WorkoutSessionState): Notification {
        val contentText = state.currentActionName?.let { name ->
            context.getString(R.string.notification_session_text, name, state.setsDone)
        } ?: context.getString(R.string.notification_session_no_action)
        val subText = state.startedAt?.let { startTime ->
            context.getString(
                R.string.notification_session_started_at,
                formatTime(startTime),
            )
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_workout_session)
            .setContentTitle(context.getString(R.string.notification_session_title))
            .setContentText(contentText)
            .setSubText(subText)
            .setContentIntent(navProvider.pendingIntentOpenToday())
            .addAction(
                0,
                context.getString(R.string.notification_action_add_set),
                navProvider.pendingIntentAddSet(),
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_finish),
                finishPendingIntent(),
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** Formats the session start time in the user's 12/24h preference. */
    private fun formatTime(instant: Instant): String =
        android.text.format.DateFormat.getTimeFormat(context)
            .format(Date(instant.toEpochMilliseconds()))

    private fun finishPendingIntent(): PendingIntent {
        val intent = Intent(context, WorkoutSessionService::class.java)
            .setAction(SessionIntents.ACTION_FINISH)
        return PendingIntent.getService(
            context,
            REQUEST_FINISH,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val CHANNEL_ID = "workout_session"
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_FINISH = 1
    }
}
