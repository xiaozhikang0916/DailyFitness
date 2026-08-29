package site.xiaozk.dailyfitness.nav

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import site.xiaozk.dailyfitness.R

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/28
 */

enum class SnackbarStatus {
    Normal,
    Error,
}

interface SnackbarDisplay {
    @get:StringRes
    val messageRes: Int
    val status: SnackbarStatus
}

object AddSuccessSnackbar : SnackbarDisplay {
    override val messageRes = R.string.snackbar_add_success
    override val status: SnackbarStatus
        get() = SnackbarStatus.Normal
}

object AddFailedSnackbar : SnackbarDisplay {
    override val messageRes = R.string.snackbar_add_failed
    override val status: SnackbarStatus
        get() = SnackbarStatus.Error
}

object DelSuccessSnackbar : SnackbarDisplay {
    override val messageRes = R.string.snackbar_del_success
    override val status: SnackbarStatus
        get() = SnackbarStatus.Normal
}

object DelFailedSnackbar : SnackbarDisplay {
    override val messageRes = R.string.snackbar_del_failed
    override val status: SnackbarStatus
        get() = SnackbarStatus.Error
}

object LoadFailedSnackbar : SnackbarDisplay {
    override val messageRes = R.string.snackbar_load_failed
    override val status: SnackbarStatus
        get() = SnackbarStatus.Error
}

/**
 * App-wide snackbar presenter, created once in AppHost.
 *
 * Pages only call the non-suspending [showSnackbar]; messages are collected and displayed
 * in AppHost where the collector coroutine survives page navigation. So a snackbar shown
 * right before popping back stack is never blocked, and keeps displaying on the page
 * underneath (all page Scaffolds bind their SnackbarHost to [snackbarHostState]).
 */
@Stable
class AppSnackbarHostState {
    val snackbarHostState = SnackbarHostState()

    private val _messages = MutableSharedFlow<SnackbarDisplay>(
        replay = 0,
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<SnackbarDisplay> = _messages.asSharedFlow()

    fun showSnackbar(display: SnackbarDisplay) {
        _messages.tryEmit(display)
    }
}
