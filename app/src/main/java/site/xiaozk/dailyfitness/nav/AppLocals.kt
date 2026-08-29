package site.xiaozk.dailyfitness.nav

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * NavBackStack shared by the whole app, provided in AppHost.
 * Pages use it directly to navigate / pop back stack.
 */
val LocalNavBackStack = compositionLocalOf<NavBackStack<NavKey>> {
    error("No NavBackStack provided, make sure AppHost wraps the content with LocalNavBackStack")
}

/**
 * App-wide snackbar presenter, provided in AppHost.
 * Pages call its non-suspending [AppSnackbarHostState.showSnackbar] directly;
 * the actual display coroutine lives in AppHost and survives page navigation.
 */
val LocalAppSnackbarHostState = compositionLocalOf<AppSnackbarHostState> {
    error("No AppSnackbarHostState provided, make sure AppHost wraps the content with LocalAppSnackbarHostState")
}
