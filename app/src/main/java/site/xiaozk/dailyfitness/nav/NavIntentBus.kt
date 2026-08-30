package site.xiaozk.dailyfitness.nav

import android.content.Intent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges intents captured by [site.xiaozk.dailyfitness.MainActivity] (cold start
 * via notification action, or onNewIntent while the app is running) into a
 * SharedFlow consumed by [site.xiaozk.dailyfitness.AppHost], which maps the
 * [site.xiaozk.dailyfitness.session.SessionIntents] action constants to NavKeys.
 *
 * SharedFlow (not StateFlow) on purpose: navigation is an event, not state -
 * no sticky replay so a restored activity never re-navigates. Buffer keeps at
 * most one pending event (DROP_OLDEST), so rapid taps lose nothing critical.
 */
@Singleton
class NavIntentBus @Inject constructor() {

    private val _intents = MutableSharedFlow<Intent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun emit(intent: Intent) {
        _intents.tryEmit(intent)
    }

    fun observe(): Flow<Intent> = _intents.asSharedFlow()
}
