package site.xiaozk.dailyfitness.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/28
 */

@OptIn(ExperimentalMaterial3Api::class)
sealed interface IScaffoldState {
    val title: String
    val titleAlign: TextAlign
        get() = TextAlign.Start
    val showBottomNavBar: Boolean

    @get:Composable
    val topAppBarColors: TopAppBarColors
    val actionItems: List<TopAction>
    val backIcon: ImageVector?
    val showFab: Boolean
        get() = false
}

/**
 * Scaffold state in home page, with title in center and bottom bar shown
 */
@OptIn(ExperimentalMaterial3Api::class)
data class HomepageScaffoldState(
    override val title: String = "",
) : IScaffoldState {
    override val showBottomNavBar: Boolean = true
    override val topAppBarColors: TopAppBarColors
        @Composable get() = TopAppBarDefaults.centerAlignedTopAppBarColors()
    override val actionItems: List<TopAction>
        get() = emptyList()
    override val backIcon: ImageVector?
        get() = null
    override val showFab: Boolean
        get() = true
    override val titleAlign: TextAlign
        get() = TextAlign.Center
}

/**
 * Scaffold state in subpage, with title in left and bottom bar hidden
 */
@OptIn(ExperimentalMaterial3Api::class)
data class SubpageScaffoldState(
    override val title: String = "",
    override val actionItems: List<TopAction> = emptyList(),
) : IScaffoldState {
    override val showBottomNavBar: Boolean = false
    override val topAppBarColors: TopAppBarColors
        @Composable get() = TopAppBarDefaults.smallTopAppBarColors()
    override val backIcon: ImageVector
        get() = Icons.Default.KeyboardArrowLeft
}

/**
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
data class FullDialogScaffoldState(
    override val title: String = "",
    override val actionItems: List<TopAction> = emptyList(),
) : IScaffoldState {
    override val showBottomNavBar: Boolean = false
    override val topAppBarColors: TopAppBarColors
        @Composable get() = TopAppBarDefaults.smallTopAppBarColors()
    override val backIcon: ImageVector
        get() = Icons.Default.Close
}

enum class SnackbarStatus {
    Normal,
    Error,
}

data class SnackbarData(
    val message: String,
    val status: SnackbarStatus = SnackbarStatus.Normal,
)


@HiltViewModel
class AppScaffoldViewModel @Inject constructor() : ViewModel() {
    val scaffoldState = MutableStateFlow<IScaffoldState?>(null)

    /**
     * route action, used to navigate between pages
     */
    val routeAction = MutableSharedFlow<Route>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * top action clicked, used to dispatch action to containing page
     */
    val topAction = MutableSharedFlow<TopAction>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val snackbarFlow = MutableSharedFlow<SnackbarData>(replay = 0, extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun setScaffoldState(state: IScaffoldState) {
        scaffoldState.value = state
    }

    fun onRoute(route: String) {
        onRoute(Route(route))
    }

    fun onRoute(route: Route) {
        routeAction.tryEmit(route)
    }

    fun back() {
        onRoute(Route.BACK)
    }

    fun onTopAction(action: TopAction) {
        topAction.tryEmit(action)
    }

    fun showSnackbar(message: String, status: SnackbarStatus = SnackbarStatus.Normal) {
        showSnackbar(SnackbarData(message, status))
    }

    fun showSnackbar(snackbarData: SnackbarData) {
        snackbarFlow.tryEmit(snackbarData)
    }

    fun showSnackbarAndBack(message: String, status: SnackbarStatus = SnackbarStatus.Normal) {
        showSnackbar(message, status)
        back()
    }
}