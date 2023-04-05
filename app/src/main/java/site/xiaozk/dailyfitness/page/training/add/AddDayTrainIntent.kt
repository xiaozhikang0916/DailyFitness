@file:OptIn(ExperimentalTypeInference::class)

package site.xiaozk.dailyfitness.page.training.add

import android.util.Log
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.base.IIntent
import site.xiaozk.dailyfitness.base.IntentResult
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit
import java.time.Instant
import javax.inject.Inject
import kotlin.experimental.ExperimentalTypeInference

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */

sealed interface IDailyTrainIntent : IIntent

object LoadPartIntent : IDailyTrainIntent

class PartLoadedIntent(val allParts: List<TrainPartGroup>) : IDailyTrainIntent

class SetInstantIntent(val instant: Instant): IDailyTrainIntent

class SelectPartIntent(val part: TrainPartGroup) : IDailyTrainIntent

class PartMenuIntent(val show: Boolean) : IDailyTrainIntent

class SelectActionIntent(val action: TrainActionWithPart) : IDailyTrainIntent

class ActionMenuIntent(val show: Boolean) : IDailyTrainIntent

class InputWeightIntent(val weight: String, val weightUnit: WeightUnit) : IDailyTrainIntent

class InputDurationIntent(val duration: String, val timeUnit: TimeUnit) : IDailyTrainIntent

class InputCountIntent(val count: String) : IDailyTrainIntent

class InputNoteIntent(val note: String) : IDailyTrainIntent

object SubmitIntent : IDailyTrainIntent

class SubmitDoneIntent(val status: ActionStatus) : IDailyTrainIntent

typealias AddDailyTrainResult = IntentResult<AddDailyWorkoutPageState, IDailyTrainIntent>

@ViewModelScoped
class DailyWorkoutReducer
@Inject constructor(
    private val repo: IDailyWorkoutRepository,
    private val trainRepo: ITrainActionRepository,
    private val userRepo: IUserRepository,
) {
    fun reduce(state: AddDailyWorkoutPageState, intent: IDailyTrainIntent): AddDailyTrainResult {
        return when (intent) {
            LoadPartIntent -> AddDailyTrainResult(state = state.copy()) {
                emitAll(trainRepo.getAllTrainParts().map { PartLoadedIntent(it) })
            }

            is PartLoadedIntent -> AddDailyTrainResult(state = AddDailyWorkoutPageState(allParts = intent.allParts, showPartMenuState = false, showActionMenuState = false))
            is SetInstantIntent -> AddDailyTrainResult(
                state = state.copy(instant = intent.instant)
            )
            is SelectPartIntent -> AddDailyTrainResult(state = state.copy(
                selectedPart = intent.part,
                showPartMenuState = false,
                showActionMenuState = false,
                selectedAction = null
            ).cleanInput()) {
                emit(
                    ActionMenuIntent(show = true)
                )
            }
            is SelectActionIntent -> AddDailyTrainResult(state = state.copy(
                selectedAction = intent.action,
                showActionMenuState = false,
                showPartMenuState = false
            ).cleanInput())
            is InputCountIntent -> AddDailyTrainResult(state = state.copy(count = intent.count))
            is InputNoteIntent -> AddDailyTrainResult(state = state.copy(note = intent.note))
            is InputDurationIntent -> AddDailyTrainResult(state = state.copy(duration = intent.duration, timeUnit = intent.timeUnit))
            is InputWeightIntent -> AddDailyTrainResult(state = state.copy(weight = intent.weight, weightUnit = intent.weightUnit))
            is PartMenuIntent -> AddDailyTrainResult(state = state.copy(showPartMenuState = intent.show, showActionMenuState = false))
            is ActionMenuIntent -> AddDailyTrainResult(state = state.copy(showActionMenuState = intent.show, showPartMenuState = false))
            SubmitIntent -> AddDailyTrainResult(state = state.copy()) {
                try {
                    repo.addWorkoutAction(userRepo.getCurrentUser(), state.toDailyTrain())
                    emit(SubmitDoneIntent(ActionStatus.Done))
                } catch (e: Exception) {
                    Log.e("AddDailyTrain", "Add daily train action failed", e)
                    emit(SubmitDoneIntent(ActionStatus.Failed(e)))
                }
            }

            is SubmitDoneIntent -> AddDailyTrainResult(state = state.copy(submitStatus = intent.status))
        }
    }
}