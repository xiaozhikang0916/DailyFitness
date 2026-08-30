package site.xiaozk.dailyfitness.session

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service backing the ongoing session notification.
 *
 * - [onStartCommand] always calls [startForeground] promptly (required after
 *   startForegroundService, including START_STICKY restarts where intent is null).
 * - a state collector is the single authority: it tracks the active->inactive
 *   transition and tears the service down only then, so a transient initial
 *   inactive value (e.g. right after ACTION_START) does not kill the service
 *   before the session becomes active.
 * - [SessionIntents.ACTION_FINISH] stops unconditionally.
 */
@AndroidEntryPoint
class WorkoutSessionService : Service() {

    @Inject
    lateinit var controller: WorkoutSessionController

    @Inject
    lateinit var notification: WorkoutSessionNotification

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        notification.createChannel()
        serviceScope.launch {
            var wasActive = false
            controller.state.collect { state ->
                if (state.active) {
                    wasActive = true
                    showNotification(state)
                } else if (wasActive) {
                    wasActive = false
                    hideAndStop()
                }
                // initial inactive while !wasActive: wait for start()/restore to kick in
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            SessionIntents.ACTION_FINISH -> {
                serviceScope.launch {
                    controller.finish()
                    hideAndStop()
                }
                return START_STICKY
            }
            SessionIntents.ACTION_START -> {
                serviceScope.launch { controller.start() }
            }
        }
        // Must show a foreground notification quickly after startForegroundService;
        // the collector updates the content as soon as the derived state arrives.
        startForeground(
            WorkoutSessionNotification.NOTIFICATION_ID,
            notification.build(controller.state.value),
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showNotification(state: WorkoutSessionState) {
        startForeground(
            WorkoutSessionNotification.NOTIFICATION_ID,
            notification.build(state),
        )
        if (hasNotificationPermission()) {
            NotificationManagerCompat.from(this)
                .notify(
                    WorkoutSessionNotification.NOTIFICATION_ID,
                    notification.build(state),
                )
        }
    }

    private fun hideAndStop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
