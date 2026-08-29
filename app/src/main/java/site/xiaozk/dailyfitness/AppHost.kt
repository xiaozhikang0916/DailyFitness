package site.xiaozk.dailyfitness

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import site.xiaozk.dailyfitness.nav.AddBodyDetail
import site.xiaozk.dailyfitness.nav.AddTrainAction
import site.xiaozk.dailyfitness.nav.AddWorkoutAction
import site.xiaozk.dailyfitness.nav.AppSnackbarHostState
import site.xiaozk.dailyfitness.nav.HomeBody
import site.xiaozk.dailyfitness.nav.HomeTrainPart
import site.xiaozk.dailyfitness.nav.HomeTraining
import site.xiaozk.dailyfitness.nav.LocalAppSnackbarHostState
import site.xiaozk.dailyfitness.nav.LocalNavBackStack
import site.xiaozk.dailyfitness.nav.TrainActionDetail
import site.xiaozk.dailyfitness.nav.TrainDay
import site.xiaozk.dailyfitness.nav.TrainPartDetail
import site.xiaozk.dailyfitness.nav.WorkoutMonth
import site.xiaozk.dailyfitness.page.action.TrainActionPage
import site.xiaozk.dailyfitness.page.action.TrainPartPage
import site.xiaozk.dailyfitness.page.action.TrainStaticPage
import site.xiaozk.dailyfitness.page.action.parts.AddTrainActionPage
import site.xiaozk.dailyfitness.page.body.BodyDetailPage
import site.xiaozk.dailyfitness.page.body.add.AddDailyBodyDetail
import site.xiaozk.dailyfitness.page.training.HomeWorkoutPage
import site.xiaozk.dailyfitness.page.training.TrainingDayDetailPage
import site.xiaozk.dailyfitness.page.training.WorkoutMonthlyPage
import site.xiaozk.dailyfitness.page.training.add.AddDailyWorkoutAction
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
 * Only hosts the NavDisplay, each page brings its own top bar / bottom bar / fab
 * via its own Scaffold. The NavBackStack and an app-wide snackbar presenter are
 * provided through composition locals for pages and dialogs; the snackbar display
 * coroutine is collected here so it survives page navigation.
 *
 * Navigation 3:
 * - type-safe [NavKey] routes, flat back stack (start = [HomeTraining])
 * - [rememberViewModelStoreNavEntryDecorator] provides a per-entry ViewModelStoreOwner
 *   (Hilt factories work inside entries, VMs are cleared when the entry is popped)
 * - dialogs are entries decorated with [DialogSceneStrategy.dialog]
 * - system back on the single start entry is NOT intercepted by NavDisplay
 *   (isBackEnabled = previousEntries.isNotEmpty()), so it falls through to the
 *   Activity default and finishes it.
 */
@Composable
fun AppHost() {
    val navBackStack: NavBackStack<NavKey> = rememberNavBackStack(HomeTraining)
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
            LocalNavBackStack provides navBackStack,
            LocalAppSnackbarHostState provides appSnackbarHostState,
        ) {
            NavDisplay(
                backStack = navBackStack,
                modifier = Modifier.fillMaxSize(),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                sceneStrategies = listOf(SinglePaneSceneStrategy()),
                entryProvider = { key ->
                    when (key) {
                        is HomeTraining -> NavEntry(key) { HomeWorkoutPage() }
                        is HomeBody -> NavEntry(key) { BodyDetailPage() }
                        is HomeTrainPart -> NavEntry(key) { TrainStaticPage() }
                        is WorkoutMonth -> NavEntry(key) { WorkoutMonthlyPage(month = key.date) }
                        is TrainDay -> NavEntry(key) { TrainingDayDetailPage(date = key.date) }
                        is AddWorkoutAction -> NavEntry(key) { AddDailyWorkoutAction() }
                        is AddBodyDetail -> NavEntry(key) { AddDailyBodyDetail() }
                        is TrainPartDetail -> NavEntry(key) { TrainPartPage(partId = key.partId) }
                        is TrainActionDetail -> NavEntry(key) { TrainActionPage(actionId = key.actionId) }
                        is AddTrainAction -> NavEntry(key) {
                            AddTrainActionPage(partId = key.partId, actionId = key.actionId)
                        }
                        else -> error("Unknown nav key: $key")
                    }
                },
            )
        }
    }
}
