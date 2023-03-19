package site.xiaozk.dailyfitness.repository.model

data class TrainActionWithPart(
    val part: TrainPart,
    val action: TrainAction = TrainAction(),
) {
    val id: Int
        get() = action.id
    val actionName: String
        get() = action.actionName
    val isTimedAction: Boolean
        get() = action.isTimedAction
    val isWeightedAction: Boolean
        get() = action.isWeightedAction
    val isCountedAction: Boolean
        get() = action.isCountedAction
}

data class TrainAction(
    val id: Int = 0,
    val partId: Int = 0,
    val actionName: String = "",
    val isTimedAction: Boolean = false,
    val isWeightedAction: Boolean = false,
    val isCountedAction: Boolean = false,
)

data class TrainPart(
    val id: Int = 0,
    val partName: String = "",
)

data class TrainPartGroup(
    val part: TrainPart = TrainPart(),
    val actions: List<TrainActionWithPart> = emptyList(),
)

data class TrainPartPage(
    val allParts: List<TrainPartGroup> = emptyList(),
)