package site.xiaozk.dailyfitness.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/28
 */
data class AppScaffoldState(
    val title: String = "",
    val showAppBar: Boolean = title.isNotBlank(),
    val fabRoute: String = "",
    val showFab: Boolean = true,
    val showBottomNavBar: Boolean = true,
)

class AppScaffoldStateBuilder {
    var title: String = ""
    var showAppBar: Boolean? = null
    var fabRoute: String = ""
    var showFab: Boolean? = null
    var showBottomNavBar: Boolean = false

    internal fun build(): AppScaffoldState {
        return AppScaffoldState(
            title = title,
            showAppBar = showAppBar ?: title.isNotBlank(),
            fabRoute = fabRoute,
            showFab = showFab ?: fabRoute.isNotBlank(),
            showBottomNavBar = showBottomNavBar
        )
    }
}

@HiltViewModel
class AppScaffoldViewModel @Inject constructor() : ViewModel() {
    val scaffoldState = MutableStateFlow(AppScaffoldState())
}

@Composable
fun IAppNavItem.updateAppScaffoldState(navController: NavController, state: AppScaffoldState) {
    val rootEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(AppHomeRootNav.route)
    }
    val appStateViewModel: AppScaffoldViewModel = hiltViewModel(rootEntry)
    SideEffect {
        if (navController.currentDestination?.route == this.route) {
            appStateViewModel.scaffoldState.value = state
        }
    }
}

@Composable
fun IAppNavItem.updateAppScaffoldState(
    navController: NavController,
    stateBuilder: AppScaffoldStateBuilder.() -> Unit,
) {
    val builder = AppScaffoldStateBuilder()
    builder.stateBuilder()
    updateAppScaffoldState(navController = navController, state = builder.build())
}