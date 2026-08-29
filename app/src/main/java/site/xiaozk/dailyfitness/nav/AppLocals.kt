package site.xiaozk.dailyfitness.nav

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavController

/**
 * NavController shared by the whole app, provided in AppHost.
 * Pages use it directly to navigate / pop back stack, no more shared scaffold ViewModel.
 */
val LocalNavController = compositionLocalOf<NavController> {
    error("No NavController provided, make sure AppHost wraps the content with LocalNavController")
}

/**
 * App-wide snackbar presenter, provided in AppHost.
 * Pages call its non-suspending [AppSnackbarHostState.showSnackbar] directly;
 * the actual display coroutine lives in AppHost and survives page navigation.
 */
val LocalAppSnackbarHostState = compositionLocalOf<AppSnackbarHostState> {
    error("No AppSnackbarHostState provided, make sure AppHost wraps the content with LocalAppSnackbarHostState")
}
