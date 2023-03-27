package site.xiaozk.dailyfitness

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import site.xiaozk.dailyfitness.nav.AppRootNav
import site.xiaozk.dailyfitness.nav.AppRootNav.appRootGraph
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.IScaffoldState
import site.xiaozk.dailyfitness.nav.IconType
import site.xiaozk.dailyfitness.nav.PageHandleAction
import site.xiaozk.dailyfitness.nav.RouteAction
import site.xiaozk.dailyfitness.nav.TextButtonType
import site.xiaozk.dailyfitness.nav.TrainPartGraph.trainPartGraph
import site.xiaozk.dailyfitness.nav.TrainingDayGroup.trainingDayGraph
import site.xiaozk.dailyfitness.theme.DailyFitnessTheme
import site.xiaozk.dailyfitness.widget.BackButton

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */

/**
 * Root compose call site of the app,
 * called in MainActivity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHost() {
    val hostNavController = rememberNavController()
    val entry = hostNavController.currentBackStackEntryAsState().value
    val appScaffoldViewModel = entry?.let { hiltViewModel<AppScaffoldViewModel>(it) }
    val scaffoldState = appScaffoldViewModel?.scaffoldState?.collectAsState()
    LaunchedEffect(key1 = appScaffoldViewModel) {
        appScaffoldViewModel?.routeAction?.collect {
            if (it.isBack) {
                hostNavController.popBackStack()
            } else {
                hostNavController.navigate(it.route)
            }
        }
    }
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    LaunchedEffect(key1 = appScaffoldViewModel) {
        appScaffoldViewModel?.snackbarFlow?.collect {
            snackbarHostState.showSnackbar(it.message)
        }
    }
    DailyFitnessTheme(darkTheme = false) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AnimatedTopBar(
                    hostNavController = hostNavController,
                    appScaffoldViewModel = appScaffoldViewModel,
                )
            },
            bottomBar = {
                BottomNavBar(
                    navController = hostNavController,
                    appScaffoldViewModel = appScaffoldViewModel,
                )
            },
            floatingActionButton = {
                if (scaffoldState?.value?.showFab == true) {
                    FloatingActionButton(onClick = {
                        /* TODO */
                    }) {
                        Image(
                            painter = rememberVectorPainter(image = Icons.Default.Add),
                            contentDescription = null
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            }
        ) {
            NavHost(
                navController = hostNavController,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AnimatedTopBar(
    hostNavController: NavHostController,
    appScaffoldViewModel: AppScaffoldViewModel?,
) {
    val scaffoldState = appScaffoldViewModel?.scaffoldState?.collectAsState()
    var rememberedState by remember {
        mutableStateOf<IScaffoldState?>(null)
    }
    if (scaffoldState?.value != null) {
        rememberedState = scaffoldState.value
    }
    AnimatedVisibility(
        visible = scaffoldState?.value != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        rememberedState?.let { state ->
            TopAppBar(
                title = {
                    Text(state.title, modifier = Modifier.fillMaxWidth(), textAlign = state.titleAlign)
                },
                colors = state.topAppBarColors,
                navigationIcon = {
                    state.backIcon?.let {
                        BackButton(icon = it) {
                            hostNavController.popBackStack()
                        }
                    }
                },
                actions = {
                    state.actionItems.forEach {
                        val onClick: () -> Unit = remember(it.actionType) {
                            {
                                when (it.actionType) {
                                    is PageHandleAction -> appScaffoldViewModel?.onTopAction(it)
                                    is RouteAction -> appScaffoldViewModel?.onRoute(it.actionType.route)
                                }
                            }
                        }
                        when (it.displayType) {
                            is TextButtonType -> TextButton(onClick = onClick, enabled = it.valid) {
                                Text(text = it.displayType.text)
                            }

                            is IconType -> {
                                val icon = it.displayType
                                IconButton(onClick = onClick, enabled = it.valid) {
                                    Icon(painter = rememberVectorPainter(image = icon.icon), contentDescription = icon.actionDesc)
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun BottomNavBar(
    navController: NavController,
    appScaffoldViewModel: AppScaffoldViewModel?,
) {
    val scaffoldState = appScaffoldViewModel?.scaffoldState?.collectAsState()
    val showNavBar = scaffoldState?.value?.showBottomNavBar ?: false
    val bottomList = remember {
        AppHomeRootNav.AppHomePage.all()
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    AnimatedVisibility(
        visible = showNavBar,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
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

