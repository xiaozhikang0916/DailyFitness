package site.xiaozk.dailyfitness

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.AndroidEntryPoint
import site.xiaozk.dailyfitness.export.AndroidExportDirectoryProvider
import site.xiaozk.dailyfitness.nav.NavIntentBus
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navIntentBus: NavIntentBus

    @Inject
    lateinit var exportDirectoryProvider: AndroidExportDirectoryProvider

    private lateinit var exportDirectoryLauncher: ActivityResultLauncher<Uri?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The system folder picker used by the data-export screen. The app layer
        // owns the platform launcher; :settings-ui only sees the provider contract.
        exportDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                exportDirectoryProvider.onDirectoryPicked(uri)
            }
        exportDirectoryProvider.attachLauncher { exportDirectoryLauncher.launch(null) }
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

    override fun onDestroy() {
        exportDirectoryProvider.detachLauncher()
        super.onDestroy()
    }
}