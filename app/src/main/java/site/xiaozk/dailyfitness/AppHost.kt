package site.xiaozk.dailyfitness

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import site.xiaozk.dailyfitness.nav.AddDailyBodyDetailNavItem.addDailyBodyDetailNav
import site.xiaozk.dailyfitness.nav.AppRootNav
import site.xiaozk.dailyfitness.nav.AppRootNav.appRootGraph
import site.xiaozk.dailyfitness.nav.AppSnackbarHostState
import site.xiaozk.dailyfitness.nav.LocalAppSnackbarHostState
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.TrainPartGraph.trainPartGraph
import site.xiaozk.dailyfitness.nav.TrainingDayGroup.trainingDayGraph
import site.xiaozk.dailyfitness.nav.WorkoutStaticGroup.workoutStaticGraph
import site.xiaozk.dailyfitness.theme.DailyFitnessTheme

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */

/**
 * Root compose call site of the app,
 * called in MainActivity.
 *
 * Only hosts the NavHost, each page brings its own top bar / bottom bar / fab
 * via its own Scaffold. The NavController and an app-wide snackbar presenter are
 * provided through composition locals for pages and dialogs; the snackbar display
 * coroutine is collected here so it survives page navigation.
 */
@Composable
fun AppHost() {
    val hostNavController = rememberNavController()
    val appSnackbarHostState = remember {
        AppSnackbarHostState()
    }
    val res = LocalResources.current
    LaunchedEffect(appSnackbarHostState, res) {
        appSnackbarHostState.messages.collect { display ->
            appSnackbarHostState.snackbarHostState.showSnackbar(
                message = res.getString(display.messageRes),
                withDismissAction = true,
            )
        }
    }
    DailyFitnessTheme(darkTheme = false) {
        CompositionLocalProvider(
            LocalNavController provides hostNavController,
            LocalAppSnackbarHostState provides appSnackbarHostState,
        ) {
            NavHost(
                navController = hostNavController,
                modifier = Modifier
                    .fillMaxSize(),
                startDestination = AppRootNav.route
            ) {
                appRootGraph()
                addDailyBodyDetailNav()
                trainPartGraph()
                trainingDayGraph()
                workoutStaticGraph()
            }
        }
    }
}
