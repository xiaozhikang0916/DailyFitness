package site.xiaozk.dailyfitness.page.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.nav.AiCoachSettings
import site.xiaozk.dailyfitness.nav.ExportData
import site.xiaozk.dailyfitness.nav.LocalAppSnackbarHostState
import site.xiaozk.dailyfitness.nav.LocalNavBackStack
import site.xiaozk.dailyfitness.settings.ui.SettingsHomeContent
import site.xiaozk.dailyfitness.widget.AppBottomBar

/**
 * App-side shell of the Settings bottom tab.
 *
 * The app owns the scaffold (top bar / bottom navigation) and navigation, while the
 * entry list itself is rendered by `:settings-ui` ([SettingsHomeContent]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomePage() {
    val navBackStack = LocalNavBackStack.current
    val appSnackbarHostState = LocalAppSnackbarHostState.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.bottom_nav_title_settings),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
            )
        },
        bottomBar = {
            AppBottomBar(backStack = navBackStack)
        },
        snackbarHost = {
            SnackbarHost(appSnackbarHostState.snackbarHostState)
        },
    ) { innerPadding ->
        SettingsHomeContent(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 12.dp,
            ),
            onOpenAiSettings = { navBackStack.add(AiCoachSettings) },
            onOpenExportData = { navBackStack.add(ExportData) },
        )
    }
}
