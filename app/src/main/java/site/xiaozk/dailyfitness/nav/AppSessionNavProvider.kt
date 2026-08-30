package site.xiaozk.dailyfitness.nav

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import site.xiaozk.dailyfitness.MainActivity
import site.xiaozk.dailyfitness.session.SessionIntents
import site.xiaozk.dailyfitness.session.WorkoutSessionNavProvider
import javax.inject.Inject

/**
 * App-side [WorkoutSessionNavProvider]: the only place that knows the app's
 * activity and NavKeys. The session module just calls these pending intents.
 */
class AppSessionNavProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : WorkoutSessionNavProvider {

    override fun pendingIntentAddSet(): PendingIntent =
        activityPendingIntent(SessionIntents.ACTION_ADD_SET, REQUEST_ADD_SET)

    override fun pendingIntentOpenToday(): PendingIntent =
        activityPendingIntent(SessionIntents.ACTION_OPEN_TODAY, REQUEST_OPEN_TODAY)

    private fun activityPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val REQUEST_ADD_SET = 1
        const val REQUEST_OPEN_TODAY = 2
    }
}
