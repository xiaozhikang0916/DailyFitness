package site.xiaozk.dailyfitness.page.action.parts

import android.util.Log
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.base.IIntent
import site.xiaozk.dailyfitness.base.IntentResult
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @create: 2023/3/28
 */
sealed interface IAddTrainActionIntent : IIntent
data class InitLoadIntent(val partId: Int, val actionId: Int) : IAddTrainActionIntent
data class SetTrainPartListIntent(val allParts: List<TrainPartGroup>) : IAddTrainActionIntent
data class SetTrainPartIntent(val trainPart: TrainPart) : IAddTrainActionIntent
data class SetTrainActionIntent(val action: TrainAction) : IAddTrainActionIntent
data class SetNameIntent(val name: String) : IAddTrainActionIntent
data class SetTimedIntent(val isTimed: Boolean) : IAddTrainActionIntent
data class SetCountedIntent(val isCounted: Boolean) : IAddTrainActionIntent
data class SetWeightedIntent(val isWeighted: Boolean) : IAddTrainActionIntent
object SubmitIntent : IAddTrainActionIntent
object SubmitFinishedIntent : IAddTrainActionIntent
data class SubmitFailedIntent(val e: Throwable) : IAddTrainActionIntent

typealias AddTrainResult = IntentResult<AddTrainActionState, IAddTrainActionIntent>

class AddTrainActionReducer @Inject constructor(
    private val repo: ITrainActionRepository,
) {
    fun reduce(
        state: AddTrainActionState,
        intent: IAddTrainActionIntent,
    ): AddTrainResult {
        return when (intent) {
            is InitLoadIntent -> AddTrainResult(state = state.copy()) {
                repo.getAllTrainParts().onEach {
                    emit(SetTrainPartListIntent(it))
                }.map { groupList ->
                    groupList.find { it.part.id == intent.partId }?.let {
                        it.takeIf { intent.actionId != 0 }
                            ?.actions
                            ?.find { action -> action.id == intent.actionId }
                            ?: TrainActionWithPart(part = it.part)
                    }
                }.collect { actionWithPart ->
                    if (actionWithPart != null) {
                        emit(SetTrainPartIntent(actionWithPart.part))
                        emit(SetTrainActionIntent(actionWithPart.action))
                    }
                }
            }

            is SetTrainPartListIntent -> AddTrainResult(state = state.copy(allPart = intent.allParts))
            is SetTrainPartIntent -> AddTrainResult(
                state = state.copy(
                    part = intent.trainPart,
                    action = TrainAction(partId = intent.trainPart.id)
                )
            )

            is SetTrainActionIntent -> AddTrainResult(
                state = if (intent.action.partId == state.part?.id) {
                    state.copy(
                        action = intent.action,
                        isTimed = intent.action.isTimedAction,
                        isCounted = intent.action.isCountedAction,
                        isWeighted = intent.action.isWeightedAction,
                    )
                } else state.copy()
            )

            is SetNameIntent -> AddTrainResult(
                state = state.copy(
                    action = state.action?.copy(
                        actionName = intent.name
                    )
                )
            )

            is SetTimedIntent -> AddTrainResult(state = state.copy(isTimed = intent.isTimed))
            is SetCountedIntent -> AddTrainResult(state = state.copy(isCounted = intent.isCounted))
            is SetWeightedIntent -> AddTrainResult(state = state.copy(isWeighted = intent.isWeighted))
            is SubmitIntent -> AddTrainResult(state = state.copy(status = ActionStatus.Loading)) {
                try {
                    val action = state.action
                    action?.apply {
                        repo.addOrUpdateTrainAction(
                            TrainAction(
                                id = id,
                                partId = state.part?.id ?: 0,
                                actionName = actionName,
                                isTimedAction = state.isTimed,
                                isWeightedAction = state.isWeighted,
                                isCountedAction = state.isCounted,
                            )
                        )
                    } ?: throw NullPointerException("Input action is null")
                    emit(SubmitFinishedIntent)
                } catch (e: Exception) {
                    Log.e("AddTrainAction", "Add action failed", e)
                    emit(SubmitFailedIntent(e))
                }
            }

            is SubmitFinishedIntent -> AddTrainResult(state = state.copy(status = ActionStatus.Done))
            is SubmitFailedIntent -> AddTrainResult(
                state = state.copy(
                    status = ActionStatus.Failed(
                        intent.e
                    )
                )
            )
        }
    }
}