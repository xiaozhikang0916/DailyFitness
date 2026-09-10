package site.xiaozk.dailyfitness.page.aicoach

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.LocalActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.aicoach.ui.AiCoachPageContent
import site.xiaozk.dailyfitness.aicoach.ui.AiCoachViewModel
import site.xiaozk.dailyfitness.nav.LocalAppSnackbarHostState
import site.xiaozk.dailyfitness.nav.LocalNavBackStack
import site.xiaozk.dailyfitness.widget.AppBottomBar

/**
 * App-side shell of the AI Coach bottom tab: top bar + bottom navigation (the
 * app owns these widgets). All AI UI comes from :ai-coach-ui via
 * [AiCoachPageContent]; this file is pure glue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCoachHomePage() {
    // Activity-scoped so the in-memory conversation survives bottom-tab switches
    // (switching tabs clears the nav back stack and its entry-scoped ViewModels).
    val activityOwner = LocalActivity.current as? ViewModelStoreOwner
    val viewModel: AiCoachViewModel = if (activityOwner != null) {
        hiltViewModel(viewModelStoreOwner = activityOwner)
    } else {
        hiltViewModel()
    }
    val state by viewModel.state.collectAsState()
    val navBackStack = LocalNavBackStack.current
    val appSnackbarHostState = LocalAppSnackbarHostState.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.bottom_nav_title_ai),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            AppBottomBar(backStack = navBackStack)
        },
        snackbarHost = {
            SnackbarHost(appSnackbarHostState.snackbarHostState)
        },
    ) { innerPadding ->
        AiCoachPageContent(
            state = state,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 12.dp,
            ),
            onRefresh = viewModel::refresh,
            onSaveConfig = viewModel::saveConfig,
        )
    }
}
