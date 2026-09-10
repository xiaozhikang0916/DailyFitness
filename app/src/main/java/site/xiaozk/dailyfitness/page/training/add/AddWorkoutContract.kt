package site.xiaozk.dailyfitness.page.training.add

import kotlin.time.Clock
import kotlin.time.Instant
import site.xiaozk.dailyfitness.aicoach.engine.CoachSuggestion
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit

/**
 * State of the "add one set" page, driven by [AddWorkoutStateMachine].
 *
 * [suggestion] is the (optional) prefill carried into the machine: it is not part
 * of the rendered form, only used once when the machine starts (M3.3) and cleared
 * afterwards.
 */
data class AddWorkoutUiState(
    val instant: Instant = Clock.System.now(),
    val allParts: List<TrainPartGroup> = emptyList(),
    val selectedPart: TrainPartGroup? = null,
    val selectedAction: TrainActionWithPart? = null,
    val showPartMenuState: Boolean = false,
    val showActionMenuState: Boolean = false,
    val duration: String = "",
    val weight: String = "",
    val count: String = "",
    val timeUnit: TimeUnit = TimeUnit.Sec,
    val weightUnit: WeightUnit = WeightUnit.Kg,
    val note: String = "",
    val submitStatus: ActionStatus = ActionStatus.Idle,
    val suggestion: CoachSuggestion? = null,
) {
    fun toDailyTrain(): DailyWorkoutAction {
        if (selectedAction != null && valid) {
            return DailyWorkoutAction(
                instant = instant,
                action = selectedAction.action,
                takenCount = count.toIntOrNull() ?: 0,
                takenDuration = duration.toFloatOrNull()?.let { RecordedDuration(it, timeUnit) },
                takenWeight = weight.toFloatOrNull()?.let { RecordedWeight(it, weightUnit) },
                note = note,
            )
        } else {
            throw IllegalStateException("Train action is not set")
        }
    }

    fun cleanInput(): AddWorkoutUiState {
        return this.copy(
            duration = "",
            weight = "",
            count = "",
            timeUnit = TimeUnit.Sec,
            weightUnit = WeightUnit.Kg,
            note = "",
        )
    }

    val timeValid: Boolean
        get() = (duration.isBlank() || duration.toFloatOrNull() != null)
    val weightValid: Boolean
        get() = (weight.isBlank() || weight.toFloatOrNull() != null)
    val countValid: Boolean
        get() = (count.isBlank() || count.toIntOrNull() != null)

    val valid: Boolean
        get() {
            return timeValid && weightValid && countValid && selectedAction != null
        }
}

/** Actions dispatched into [AddWorkoutStateMachine] by the page. */
sealed interface AddWorkoutAction {
    data class SelectPart(
        val part: TrainPartGroup,
        val openActionSelection: Boolean = true,
    ) : AddWorkoutAction

    data class SelectAction(val action: TrainActionWithPart) : AddWorkoutAction

    data class SetInstant(val instant: Instant) : AddWorkoutAction

    data class PartMenu(val show: Boolean) : AddWorkoutAction

    data class ActionMenu(val show: Boolean) : AddWorkoutAction

    data class InputWeight(val weight: String, val weightUnit: WeightUnit) : AddWorkoutAction

    data class InputDuration(val duration: String, val timeUnit: TimeUnit) : AddWorkoutAction

    data class InputCount(val count: String) : AddWorkoutAction

    data class InputNote(val note: String) : AddWorkoutAction

    data object Submit : AddWorkoutAction
}

/** Renders a Float without a trailing ".0" for whole values. */
internal fun formatRecordedNumber(value: Float): String =
    if (value % 1.0f == 0.0f) value.toLong().toString() else value.toString()
