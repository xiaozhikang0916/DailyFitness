package site.xiaozk.dailyfitness

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.filterNotNull
import site.xiaozk.dailyfitness.nav.AddDailyBodyDetailNavItem.addDailyBodyDetailNav
import site.xiaozk.dailyfitness.nav.AppHomeRootNav
import site.xiaozk.dailyfitness.nav.AppRootNav
import site.xiaozk.dailyfitness.nav.AppRootNav.appRootGraph
import site.xiaozk.dailyfitness.nav.AppScaffoldViewModel
import site.xiaozk.dailyfitness.nav.IScaffoldState
import site.xiaozk.dailyfitness.nav.IconType
import site.xiaozk.dailyfitness.nav.LocalScaffoldProperty
import site.xiaozk.dailyfitness.nav.PageHandleAction
import site.xiaozk.dailyfitness.nav.Route
import site.xiaozk.dailyfitness.nav.RouteAction
import site.xiaozk.dailyfitness.nav.ScaffoldProperty
import site.xiaozk.dailyfitness.nav.TextButtonType
import site.xiaozk.dailyfitness.nav.TrainPartGraph.trainPartGraph
import site.xiaozk.dailyfitness.nav.TrainingDayGroup.trainingDayGraph
import site.xiaozk.dailyfitness.nav.WorkoutStaticGroup.workoutStaticGraph
import site.xiaozk.dailyfitness.nav.localAppScaffoldViewModel
import site.xiaozk.dailyfitness.theme.DailyFitnessTheme
import site.xiaozk.dailyfitness.widget.BackButton
import site.xiaozk.dailyfitness.widget.HostFab

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
    val appScaffoldViewModel = localAppScaffoldViewModel()
    val scaffoldState = remember {
        mutableStateOf<IScaffoldState?>(null)
    }
    appScaffoldViewModel.scaffoldState.collectAsState().value?.let {
        scaffoldState.value = it
    }
    LaunchedEffect(key1 = appScaffoldViewModel) {
        appScaffoldViewModel.routeAction.collect {
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
        appScaffoldViewModel.snackbarFlow.filterNotNull().collect {
            snackbarHostState.showSnackbar(message = it.message, withDismissAction = true)
        }
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    DailyFitnessTheme(darkTheme = false) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            topBar = {
                AnimatedTopBar(
                    appScaffoldViewModel = appScaffoldViewModel,
                    scrollBehavior = scroll
                ) {
                    appScaffoldViewModel.onRoute(it)
                }
            },
            bottomBar = {
                BottomNavBar(
                    navController = hostNavController,
                    appScaffoldViewModel = appScaffoldViewModel,
                )
            },
            floatingActionButton = {
                HostFab(scaffoldState = scaffoldState.value, topAppBarState = scroll.state) {
                    appScaffoldViewModel.onRoute(it)
                }
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            }
        ) {
            CompositionLocalProvider(
                LocalScaffoldProperty provides ScaffoldProperty(it, scroll.nestedScrollConnection)
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
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AnimatedTopBar(
    appScaffoldViewModel: AppScaffoldViewModel?,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    onRoute: (Route) -> Unit,
) {
    val scaffoldState = appScaffoldViewModel?.scaffoldState?.collectAsState()
    var rememberedState by remember {
        mutableStateOf<IScaffoldState?>(null)
    }
    if (scaffoldState?.value != null) {
        rememberedState = scaffoldState.value
    }
    AnimatedVisibility(
        visible = rememberedState?.showTopBar == true,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        rememberedState?.let { state ->
            val title: @Composable () -> Unit = {
                Text(state.title, modifier = Modifier.fillMaxWidth(), textAlign = state.titleAlign)
            }
            val navigation: @Composable () -> Unit = {
                state.backIcon?.let {
                    BackButton(icon = it) {
                        onRoute(Route.BACK)
                    }
                }
            }
            val actions: @Composable RowScope.() -> Unit = {
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
                                Icon(
                                    painter = rememberVectorPainter(image = icon.icon),
                                    contentDescription = icon.actionDesc
                                )
                            }
                        }
                    }
                }
            }
            if (state.topAppBarCentered) {
                CenterAlignedTopAppBar(
                    title = title,
                    navigationIcon = navigation,
                    actions = actions,
                    scrollBehavior = scrollBehavior
                )
            } else {
                TopAppBar(
                    title = title,
                    navigationIcon = navigation,
                    actions = actions,
                    scrollBehavior = scrollBehavior
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(
    navController: NavController,
    appScaffoldViewModel: AppScaffoldViewModel?,
) {
    val scaffoldState = appScaffoldViewModel?.scaffoldState?.collectAsState()
    var rememberedState by remember {
        mutableStateOf<IScaffoldState?>(null)
    }
    if (scaffoldState?.value != null) {
        rememberedState = scaffoldState.value
    }
    val showNavBar = rememberedState?.showBottomNavBar ?: false
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

