package site.xiaozk.dailyfitness.page.training.add

import com.freeletics.flowredux2.FlowReduxStateMachineFactory
import com.freeletics.flowredux2.initializeWith
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import site.xiaozk.dailyfitness.aicoach.engine.CoachSuggestion
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit

/**
 * FlowRedux state machine of the "add one set" page (M3.3-0).
 *
 * Replaces the previous hand-rolled `DailyWorkoutReducer` + `IDailyTrainIntent`.
 * The optional [suggestion] is an assisted dependency: it is baked into the
 * initial [AddWorkoutUiState] and, once the machine starts, is either used to
 * prefill the form (M3.3 "adopt") or, when `null`, the last recorded action is
 * loaded instead.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddWorkoutStateMachine @AssistedInject constructor(
    private val workoutRepository: IDailyWorkoutRepository,
    private val trainRepository: ITrainActionRepository,
    private val userRepository: IUserRepository,
    @Assisted private val suggestion: CoachSuggestion?,
) : FlowReduxStateMachineFactory<AddWorkoutUiState, AddWorkoutAction>() {

    @AssistedFactory
    interface Factory {
        fun create(suggestion: CoachSuggestion?): AddWorkoutStateMachine
    }

    init {
        initializeWith(reuseLastEmittedStateOnLaunch = false) {
            AddWorkoutUiState(suggestion = suggestion)
        }
        spec {
            inState<AddWorkoutUiState> {
                // Runs once when the machine enters its initial state: load the action
                // library, then either apply the route suggestion or fall back to the
                // last recorded action.
                onEnter {
                    val parts = runCatching { trainRepository.getAllTrainParts().first() }
                        .getOrDefault(emptyList())
                    val base = snapshot
                    val prefill = base.suggestion
                    val target = if (prefill != null) {
                        base.withSuggestion(prefill, parts)
                    } else {
                        val last = runCatching {
                            workoutRepository.getLastWorkout(userRepository.getCurrentUser())
                        }.getOrNull()
                        if (last != null) base.withLastWorkout(last, parts) else base.copy(allParts = parts)
                    }
                    override { target.copy(suggestion = null) }
                }

                on<AddWorkoutAction.SelectPart> { action ->
                    mutate {
                        copy(
                            selectedPart = action.part,
                            selectedAction = null,
                            showPartMenuState = false,
                            showActionMenuState = action.openActionSelection,
                        ).cleanInput()
                    }
                }
                on<AddWorkoutAction.SelectAction> { action ->
                    mutate {
                        copy(
                            selectedAction = action.action,
                            showPartMenuState = false,
                            showActionMenuState = false,
                        ).cleanInput()
                    }
                }
                on<AddWorkoutAction.SetInstant> { action ->
                    mutate { copy(instant = action.instant) }
                }
                on<AddWorkoutAction.PartMenu> { action ->
                    mutate { copy(showPartMenuState = action.show, showActionMenuState = false) }
                }
                on<AddWorkoutAction.ActionMenu> { action ->
                    mutate { copy(showActionMenuState = action.show, showPartMenuState = false) }
                }
                on<AddWorkoutAction.InputWeight> { action ->
                    mutate { copy(weight = action.weight, weightUnit = action.weightUnit) }
                }
                on<AddWorkoutAction.InputDuration> { action ->
                    mutate { copy(duration = action.duration, timeUnit = action.timeUnit) }
                }
                on<AddWorkoutAction.InputCount> { action ->
                    mutate { copy(count = action.count) }
                }
                on<AddWorkoutAction.InputNote> { action ->
                    mutate { copy(note = action.note) }
                }
                on<AddWorkoutAction.Submit> {
                    val result = runCatching {
                        workoutRepository.addWorkoutAction(
                            userRepository.getCurrentUser(),
                            snapshot.toDailyTrain(),
                        )
                    }
                    val status = result.fold(
                        onSuccess = { ActionStatus.Done },
                        onFailure = { ActionStatus.Failed(it) },
                    )
                    mutate { copy(submitStatus = status) }
                }
            }
        }
    }
}

private fun AddWorkoutUiState.withSuggestion(
    suggestion: CoachSuggestion,
    parts: List<TrainPartGroup>,
): AddWorkoutUiState {
    val part = suggestion.partName
        ?.let { name -> parts.firstOrNull { it.part.partName == name } }
        ?: parts.firstOrNull { group -> group.actions.any { it.action.actionName == suggestion.actionName } }
    val action = part?.actions?.firstOrNull { it.action.actionName == suggestion.actionName }
    if (part == null || action == null) return copy(allParts = parts)
    return copy(
        allParts = parts,
        selectedPart = part,
        selectedAction = action,
        showPartMenuState = false,
        showActionMenuState = false,
        count = suggestion.reps?.toString().orEmpty(),
        duration = suggestion.durationSec?.toString().orEmpty(),
        weight = suggestion.weightKg?.let { formatRecordedNumber(it.toFloat()) }.orEmpty(),
        timeUnit = TimeUnit.Sec,
        weightUnit = WeightUnit.Kg,
        note = "",
    )
}

private fun AddWorkoutUiState.withLastWorkout(
    last: DailyWorkoutAction,
    parts: List<TrainPartGroup>,
): AddWorkoutUiState {
    val part = parts.firstOrNull { it.part.id == last.action.partId }
    val action = part?.actions?.firstOrNull { it.action.id == last.action.id }
    if (part == null || action == null) return copy(allParts = parts)
    return copy(
        allParts = parts,
        selectedPart = part,
        selectedAction = action,
        showPartMenuState = false,
        showActionMenuState = false,
        count = last.takenCount.takeIf { it > 0 }?.toString().orEmpty(),
        duration = last.takenDuration?.let { formatRecordedNumber(it.duration) }.orEmpty(),
        weight = last.takenWeight?.let { formatRecordedNumber(it.weight) }.orEmpty(),
        timeUnit = last.takenDuration?.timeUnit ?: TimeUnit.Sec,
        weightUnit = last.takenWeight?.weightUnit ?: WeightUnit.Kg,
        note = last.note,
    )
}
