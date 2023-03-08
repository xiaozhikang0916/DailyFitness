package site.xiaozk.dailyfitness.repository.model

data class TrainAction(
    val id: Int = 0,
    val actionName: String = "",
    val part: TrainPart,
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
    val actions: List<TrainAction> = emptyList(),
)

data class TrainPartPage(
    val allParts: List<TrainPartGroup> = emptyList(),
)