package site.xiaozk.dailyfitness

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import site.xiaozk.dailyfitness.nav.AddDailyBodyDetailNavItem.addDailyBodyDetailNav
import site.xiaozk.dailyfitness.nav.AppHomeRootNav
import site.xiaozk.dailyfitness.nav.AppHomeRootNav.AppHomePage.TrainPartNavItem.homeGraph
import site.xiaozk.dailyfitness.nav.AppRootNav
import site.xiaozk.dailyfitness.nav.AppRootNav.appRootGraph
import site.xiaozk.dailyfitness.nav.AppScaffoldState
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.LocalNavController
import site.xiaozk.dailyfitness.nav.TrainPartGraph.trainPartGraph
import site.xiaozk.dailyfitness.nav.TrainingDayGroup.trainingDayGraph
import site.xiaozk.dailyfitness.nav.provides
import site.xiaozk.dailyfitness.theme.DailyFitnessTheme

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */

/**
 * Root compose call site of the app,
 * called in MainActivity
 */
@Composable
fun AppHost() {
    val hostNavController = rememberNavController()

    DailyFitnessTheme(darkTheme = false) {
        CompositionLocalProvider(
            hostNavController.provides()
        ) {
            NavHost(
                navController = hostNavController,
                modifier = Modifier.fillMaxSize(),
                startDestination = AppRootNav.route
            ) {
                appRootGraph()
                addDailyBodyDetailNav()
                trainPartGraph()
                trainingDayGraph()
            }
        }
    }
}


/**
 * Serve as homepage of the app,
 * host a bottom nav bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHome(bottomNavController: NavHostController) {
    val current = bottomNavController.currentBackStackEntryFlow.collectAsState(initial = null).value
    val rootEntry = if (current != null) {
        remember(current) {
            bottomNavController.getBackStackEntry(AppHomeRootNav.route)
        }
    } else {
        null
    }
    val appScaffoldState = if (rootEntry != null) {
        hiltViewModel<AppScaffoldViewModel>(rootEntry).scaffoldState.collectAsState().value
    } else {
        AppScaffoldState()
    }
    LaunchedEffect(key1 = appScaffoldState) {
        Log.i("HostPage", "Current app state $appScaffoldState")
    }
    CompositionLocalProvider(
        bottomNavController.provides()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                BottomNavBar(
                    navController = bottomNavController,
                    showNavBar = appScaffoldState.showBottomNavBar
                )
            },
            floatingActionButton = {
                val nav = LocalNavController.current
                if (appScaffoldState.showFab) {
                    FloatingActionButton(onClick = {
                        nav?.navigate(
                            appScaffoldState.fabRoute
                        )
                    }) {
                        Image(
                            painter = rememberVectorPainter(image = Icons.Default.Add),
                            contentDescription = null
                        )
                    }
                }
            },
        ) {
            NavHost(
                navController = bottomNavController,
                modifier = Modifier.padding(it),
                startDestination = AppHomeRootNav.route
            ) {
                homeGraph()
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, showNavBar: Boolean) {
    val bottomList = remember {
        AppHomeRootNav.AppHomePage.all()
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    if (showNavBar) {
        NavigationBar {
            bottomList.forEach {
                NavigationBarItem(
                    selected = currentDestination?.hierarchy?.any { dest ->
                        it.route == dest.route
                    } == true,
                    onClick = {
                        navController.navigate(it.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    },
                    icon = { Icon(imageVector = it.icon, contentDescription = null) },
                    label = { Text(text = it.getName(LocalContext.current)) })
            }
        }
    }
}

