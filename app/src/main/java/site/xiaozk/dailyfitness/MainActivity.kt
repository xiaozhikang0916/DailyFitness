package site.xiaozk.dailyfitness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import site.xiaozk.dailyfitness.nav.NavIntentBus
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navIntentBus: NavIntentBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // First creation only: forward the launching intent (e.g. a notification
        // action tap). onNewIntent covers the app-already-running case;
        // recreation (rotation / process death) restores via saved state instead,
        // so no duplicate navigation events are emitted.
        if (savedInstanceState == null) {
            navIntentBus.emit(intent)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppHost(navIntentBus = navIntentBus)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navIntentBus.emit(intent)
    }
}