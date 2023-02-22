@file:OptIn(ExperimentalTypeInference::class)

package site.xiaozk.dailyfitness.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlin.experimental.ExperimentalTypeInference

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */

interface IIntent

class IntentResult<State, Intent : IIntent>(
    val state: State,
    val sideEffect: Flow<Intent> = emptyFlow(),
) {
    constructor(
        state: State,
        @BuilderInference sideEffectBuilder: suspend FlowCollector<Intent>.() -> Unit,
    ) : this(state, flow(sideEffectBuilder))
}

sealed interface ActionStatus {
    object Idle: ActionStatus
    object Loading: ActionStatus
    object Done: ActionStatus
    class Failed(e: Throwable): ActionStatus
}