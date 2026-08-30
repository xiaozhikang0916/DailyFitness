@file:OptIn(ExperimentalMaterial3Api::class)

package site.xiaozk.dailyfitness.widget

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import site.xiaozk.dailyfitness.nav.HomeTab
import site.xiaozk.dailyfitness.nav.LocalAppSnackbarHostState
import site.xiaozk.dailyfitness.nav.LocalNavBackStack

/**
 * Insets + nested scroll connection of the page-level Scaffold, passed down to the page
 * content so it can pad itself and drive the collapsing top bar.
 */
@Immutable
class ScaffoldProperty(
    val padding: PaddingValues = PaddingValues(),
    val scrollConnection: NestedScrollConnection = EmptyScrollConnection,
)

private object EmptyScrollConnection : NestedScrollConnection

/**
 * Scaffold for home pages: centered title top bar + bottom navigation bar + fab.
 */
@Composable
fun HomePageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    sessionActive: Boolean = false,
    onStartWorkout: () -> Unit = {},
    onFinishWorkout: () -> Unit = {},
    content: @Composable (ScaffoldProperty) -> Unit,
) {
    val navBackStack = LocalNavBackStack.current
    val appSnackbarHostState = LocalAppSnackbarHostState.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
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
        floatingActionButton = {
            HostFab(
                backStack = navBackStack,
                topAppBarState = scrollBehavior.state,
                sessionActive = sessionActive,
                onStartWorkout = onStartWorkout,
                onFinishWorkout = onFinishWorkout,
            )
        },
        snackbarHost = {
            SnackbarHost(appSnackbarHostState.snackbarHostState)
        }
    ) { innerPadding ->
        content(ScaffoldProperty(innerPadding, scrollBehavior.nestedScrollConnection))
    }
}

/**
 * Scaffold for sub pages: left aligned title + back button + optional top bar actions.
 */
@Composable
fun SubPageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (ScaffoldProperty) -> Unit,
) {
    val appSnackbarHostState = LocalAppSnackbarHostState.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = title,
                onBack = onBack,
                backIcon = backIcon,
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(appSnackbarHostState.snackbarHostState)
        }
    ) { innerPadding ->
        content(ScaffoldProperty(innerPadding, scrollBehavior.nestedScrollConnection))
    }
}

/**
 * Scaffold for full screen dialog-like pages: left aligned title + close button + actions.
 */
@Composable
fun DialogPageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (ScaffoldProperty) -> Unit,
) {
    SubPageScaffold(
        title = title,
        onBack = onBack,
        modifier = modifier,
        backIcon = Icons.Default.Close,
        actions = actions,
        content = content,
    )
}

@Composable
private fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = title) },
        navigationIcon = {
            BackButton(icon = backIcon, onBackClick = onBack)
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun AppBottomBar(backStack: NavBackStack<NavKey>) {
    val tabs = remember {
        HomeTab.entries
    }
    val current = backStack.lastOrNull()
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.route,
                // A1 decision: switching tabs clears the back stack (and with it any
                // entry-scoped ViewModels); data is reloaded on each switch.
                onClick = {
                    backStack.clear()
                    backStack.add(tab.route)
                },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(text = stringResource(tab.labelRes)) })
        }
    }
}
