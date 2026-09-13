package site.xiaozk.dailyfitness

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
        // Go edge-to-edge: draw behind the system bars and handle insets in Compose.
        // The app is light-themed only, so keep fully transparent bars with dark
        // (light-colored) system bar icons. This replaces Accompanist's
        // SystemUiController, which was used only to set the bar colors.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            AppHost(navIntentBus = navIntentBus)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navIntentBus.emit(intent)
    }
}