package site.xiaozk.dailyfitness.aicoach

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Instant
import site.xiaozk.dailyfitness.aicoach.llm.NextAdviceReply
import site.xiaozk.dailyfitness.aicoach.llm.PartPlanReply
import site.xiaozk.dailyfitness.aicoach.llm.PlanExecutor
import site.xiaozk.dailyfitness.repository.IAiCoachConfigStore
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.AiCoachConfig
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutAction
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutListActionPair
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutMap
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.HomeWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.MonthWorkoutStatic
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.User
import site.xiaozk.dailyfitness.repository.model.unit.RecordedDuration
import site.xiaozk.dailyfitness.repository.model.unit.RecordedWeight
import site.xiaozk.dailyfitness.repository.model.unit.TimeUnit
import site.xiaozk.dailyfitness.repository.model.unit.WeightUnit

// ---------------------------------------------------------------------------
// Minimal repository model builders.
// ---------------------------------------------------------------------------

data class TestActionSpec(
    val name: String,
    val weighted: Boolean = false,
    val counted: Boolean = false,
    val timed: Boolean = false,
)

data class TestPartSpec(
    val name: String,
    val actions: List<TestActionSpec>,
)

data class TestSetSpec(
    val weight: Double? = null,
    val weightUnit: WeightUnit = WeightUnit.Kg,
    val reps: Int? = null,
    val duration: Int? = null,
    val durationUnit: TimeUnit = TimeUnit.Sec,
)

/** One logical action block; many sets become one [DailyWorkoutListActionPair]. */
data class TestDayAction(
    val part: String,
    val action: String,
    val weighted: Boolean = false,
    val counted: Boolean = false,
    val timed: Boolean = false,
    val sets: List<TestSetSpec> = listOf(TestSetSpec()),
)

fun todayLocalDate(): LocalDate =
    kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())

/**
 * Real-time polling helper for cross-thread conditions (production stateIn/observer
 * scopes run on Dispatchers.Default, so they are not scheduled on the runTest
 * virtual dispatcher). Thread.sleep on a Default worker avoids virtual-time
 * starvation inside runTest.
 */
suspend fun awaitUntil(
    timeoutMs: Long = 5_000,
    condition: () -> Boolean,
) {
    val deadline = System.nanoTime() + timeoutMs * 1_000_000L
    withContext(Dispatchers.Default) {
        while (!condition()) {
            if (System.nanoTime() > deadline) {
                error("awaitUntil timed out after ${timeoutMs}ms")
            }
            Thread.sleep(10)
        }
    }
}

fun daysAgo(n: Int): LocalDate = todayLocalDate().minus(n, DateTimeUnit.DAY)

/** Builds TrainPartGroup list mirroring [parts]; ids are stable per index. */
fun trainGroups(parts: List<TestPartSpec>): List<TrainPartGroup> =
    parts.mapIndexed { partIndex, spec ->
        TrainPartGroup(
            part = TrainPart(id = partIndex + 1, partName = spec.name),
            actions = spec.actions.mapIndexed { actionIndex, actionSpec ->
                TrainActionWithPart(
                    part = TrainPart(id = partIndex + 1, partName = spec.name),
                    action = TrainAction(
                        id = actionIndex + 1,
                        partId = partIndex + 1,
                        actionName = actionSpec.name,
                        isTimedAction = actionSpec.timed,
                        isWeightedAction = actionSpec.weighted,
                        isCountedAction = actionSpec.counted,
                    ),
                )
            },
        )
    }

fun simpleGroups(vararg names: String): List<TrainPartGroup> =
    trainGroups(names.map { TestPartSpec(it, listOf(TestActionSpec("${it}主练动作", weighted = true, counted = true))) })

/** Typical chest part library used by several scenario tests (fake & real LLM). */
fun chestGroups(): List<TrainPartGroup> = trainGroups(
    listOf(
        TestPartSpec(
            "胸部",
            listOf(
                TestActionSpec("卧推", weighted = true, counted = true),
                TestActionSpec("哑铃飞鸟", weighted = true, counted = true),
            ),
        )
    )
)

/** One workout day from [entries]. */
fun workoutOf(date: LocalDate, vararg entries: TestDayAction): DailyWorkout {
    val actions = entries.mapIndexed { entryIndex, entry ->
        val part = TrainPart(id = entryIndex + 1, partName = entry.part)
        val dayMs = date.toEpochDays() * 86_400_000L
        val trainAction = TrainAction(
            id = 1,
            partId = part.id,
            actionName = entry.action,
            isTimedAction = entry.timed,
            isWeightedAction = entry.weighted,
            isCountedAction = entry.counted,
        )
        val sets = entry.sets.mapIndexed { index, set ->
            DailyWorkoutAction(
                id = index + 1,
                instant = Instant.fromEpochMilliseconds(dayMs + index * 60_000L),
                action = trainAction,
                takenWeight = set.weight?.let {
                    RecordedWeight(weight = it.toFloat(), weightUnit = set.weightUnit)
                },
                takenCount = set.reps ?: 0,
                takenDuration = set.duration?.let {
                    RecordedDuration(duration = it.toFloat(), timeUnit = set.durationUnit)
                },
                note = "",
            )
        }
        DailyWorkoutListActionPair(
            TrainActionWithPart(part = part, action = trainAction),
            sets,
        )
    }
    return DailyWorkout(date = date, actions = actions)
}

fun workoutMap(vararg days: DailyWorkout): DailyWorkoutMap {
    val map = HashMap<LocalDate, DailyWorkout>()
    days.forEach { map[it.date] = it }
    return DailyWorkoutMap(map)
}

// ---------------------------------------------------------------------------
// Fakes for repository interfaces & the LLM seam.
// ---------------------------------------------------------------------------

class FakeConfigStore(initial: AiCoachConfig = AiCoachConfig()) : IAiCoachConfigStore {
    private val state = MutableStateFlow(initial)
    override fun observe(): Flow<AiCoachConfig> = state
    override suspend fun save(config: AiCoachConfig) {
        state.value = config
    }
}

class FakeUserRepository : IUserRepository {
    override suspend fun getCurrentUser(): User = User(uid = 1, name = "test")
    override suspend fun createUser(user: User) = Unit
}

class FakeWorkoutRepository(
    private val map: DailyWorkoutMap,
) : IDailyWorkoutRepository {
    override fun getAllWorkoutDayList(user: User): Flow<DailyWorkoutMap> = flowOf(map)
    override fun getWorkoutDayList(
        user: User,
        from: LocalDate,
        to: LocalDate,
    ): Flow<DailyWorkoutMap> = flowOf(map)
    override fun getMonthWorkoutStatic(user: User, month: YearMonth): Flow<MonthWorkoutStatic> = TODO()
    override fun getHomeWorkoutStatics(user: User, month: YearMonth): Flow<HomeWorkoutStatic> = TODO()
    override suspend fun getWorkout(user: User, workoutId: Int): DailyWorkoutAction = TODO()
    override suspend fun addWorkoutAction(user: User, action: DailyWorkoutAction) = TODO()
    override suspend fun deleteWorkoutAction(user: User, action: DailyWorkoutAction) = TODO()
    override suspend fun getLastWorkout(
        user: User,
        date: LocalDate,
        zoneId: TimeZone,
    ): DailyWorkoutAction? = TODO()
}

class FakeTrainActionRepository(
    private val groups: List<TrainPartGroup>,
) : ITrainActionRepository {
    override fun getAllTrainParts(): Flow<List<TrainPartGroup>> = flowOf(groups)
    override fun getAllTrainPartStatics(): Flow<HomeTrainPartPage> = TODO()
    override fun getTrainPartStatic(partId: Int): Flow<TrainPartStaticPage?> = TODO()
    override fun getTrainActionStatic(actionId: Int): Flow<TrainActionStaticPage?> = TODO()
    override fun getActionsOfPart(partId: Int): Flow<TrainPartGroup> = TODO()
    override fun getAction(actionId: Int): Flow<TrainAction> = TODO()
    override suspend fun addTrainPart(part: TrainPart) = TODO()
    override suspend fun updateTrainPart(part: TrainPart) = TODO()
    override suspend fun removeTrainPart(part: TrainPart) = TODO()
    override suspend fun addTrainAction(action: TrainAction) = TODO()
    override suspend fun updateTrainAction(action: TrainAction) = TODO()
    override suspend fun removeTrainAction(action: TrainAction) = TODO()
}

/** Scripted [PlanExecutor]: queue replies per prompt id, records (promptId, userText). */
class FakePlanExecutor : PlanExecutor {
    val calls = mutableListOf<Pair<String, String>>()
    private val partPlans = ArrayDeque<Result<PartPlanReply>>()
    private val nextAdvices = ArrayDeque<Result<NextAdviceReply>>()

    fun enqueuePartPlan(result: Result<PartPlanReply>) = partPlans.addLast(result)
    fun enqueueNextAdvice(result: Result<NextAdviceReply>) = nextAdvices.addLast(result)

    val partPlanRequests: List<String>
        get() = calls.filter { it.first == "aicoach-part-plan" }.map { it.second }
    val nextAdviceRequests: List<String>
        get() = calls.filter { it.first == "aicoach-next-advice" }.map { it.second }

    override suspend fun <T> request(
        promptId: String,
        systemText: String,
        userText: String,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): Result<T> {
        calls += promptId to userText
        val reply: Result<*> = when (promptId) {
            "aicoach-part-plan" -> partPlans.removeFirstOrNull()
                ?: Result.failure(IllegalStateException("no scripted part-plan reply"))
            "aicoach-next-advice" -> nextAdvices.removeFirstOrNull()
                ?: Result.failure(IllegalStateException("no scripted next-advice reply"))
            else -> Result.failure(IllegalStateException("unexpected prompt id $promptId"))
        }
        @Suppress("UNCHECKED_CAST")
        return reply as Result<T>
    }

    override fun close() = Unit
}
