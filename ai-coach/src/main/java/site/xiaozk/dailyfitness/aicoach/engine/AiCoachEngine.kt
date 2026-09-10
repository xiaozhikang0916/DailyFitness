package site.xiaozk.dailyfitness.aicoach.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import site.xiaozk.dailyfitness.aicoach.config.AiCoachConfigProvider
import site.xiaozk.dailyfitness.aicoach.llm.AdviceKindReply
import site.xiaozk.dailyfitness.aicoach.llm.NextAdviceReply
import site.xiaozk.dailyfitness.aicoach.llm.PartPlanReply
import site.xiaozk.dailyfitness.aicoach.llm.PlanExecutor
import site.xiaozk.dailyfitness.aicoach.prompt.AiPrompts
import site.xiaozk.dailyfitness.aicoach.prompt.HistorySummarizer
import site.xiaozk.dailyfitness.aicoach.prompt.normalizeForMatch
import site.xiaozk.dailyfitness.repository.IDailyWorkoutRepository
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.DailyWorkout
import site.xiaozk.dailyfitness.repository.model.DailyWorkoutMap
import site.xiaozk.dailyfitness.repository.model.TrainPartGroup
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Orchestrates the AI Coach flows.
 *
 * Stateless with respect to the conversation: the caller passes its in-memory
 * [CoachMessage] history (engine keeps only the last [HISTORY_MESSAGES] = 5
 * rounds) and receives the new turn(s) inside the successful results. The LLM is
 * reached through [PlanExecutor] (client cache rebuilt on config change), so the
 * whole engine stays unit-testable with a fake executor (no network).
 */
@Singleton
class AiCoachEngine @Inject constructor(
    private val configProvider: AiCoachConfigProvider,
    private val userRepository: IUserRepository,
    private val workoutRepository: IDailyWorkoutRepository,
    private val trainRepository: ITrainActionRepository,
    private val planExecutor: PlanExecutor,
) : IAiCoach {

    private val gate = Mutex()

    override suspend fun recommendToday(history: List<CoachMessage>): AiCoachResult = gate.withLock {
        if (!configProvider.current.configured) {
            return@withLock AiCoachResult.ConfigMissing
        }
        val conversation = history.takeLast(HISTORY_MESSAGES)
        val user = userRepository.getCurrentUser()
        val today = todayLocalDate()
        val allWorkouts = workoutRepository.getAllWorkoutDayList(user).first()
        val trainGroups = trainRepository.getAllTrainParts().first()
        if (trainGroups.isEmpty()) {
            return@withLock AiCoachResult.NoTrainParts
        }

        val todayWorkout = allWorkouts[today]
        return@withLock if (todayWorkout?.actions?.isNotEmpty() == true) {
            nextAdvice(today, allWorkouts, todayWorkout, trainGroups, conversation)
        } else {
            planToday(today, allWorkouts, trainGroups, conversation)
        }
    }

    // ------------------------------------------------------------------ Case A

    private suspend fun planToday(
        today: LocalDate,
        allWorkouts: DailyWorkoutMap,
        trainGroups: List<TrainPartGroup>,
        history: List<CoachMessage>,
    ): AiCoachResult {
        // All past sessions, oldest -> newest.
        val historyDays = allWorkouts.trainedDate.entries
            .filter { it.key < today }
            .sortedBy { it.key }
            .mapNotNull { HistorySummarizer.summarize(it.value, today) }

        if (historyDays.isEmpty()) {
            // No history at all: the model must answer with a plan (needMore is forbidden).
            val userText = AiPrompts.partPlanUser(trainGroups, emptyList())
            val reply = planExecutor.request(
                "aicoach-part-plan", AiPrompts.partPlanSystem(), userText, history, PartPlanReply.serializer(),
            ).getOrElse { return AiCoachResult.Failed(userFacingError(it), retryable = true) }
            if (reply.needMore || reply.plan.isNullOrEmpty()) {
                return AiCoachResult.Failed(
                    "AI 在没有任何历史可提供时仍请求更多数据或返回了空计划，请重试。",
                    retryable = true,
                )
            }
            val mapped = mapPlan(reply, trainGroups, sessionsUsed = 0, rounds = 1)
            return if (mapped is AiCoachResult.TodayPlan) {
                mapped.copy(
                    newMessages = turn(
                        userLabel = planRequestLabel(today, sessionsUsed = 0),
                        assistantLabel = planSummary(mapped.parts),
                    )
                )
            } else {
                mapped
            }
        }

        var included = min(INITIAL_SESSIONS, historyDays.size)
        var rounds = 0
        var forceFallback = false

        while (true) {
            rounds++
            val window = historyDays.subList(max(0, historyDays.size - included), historyDays.size)
            val additionalNote = if (forceFallback) {
                "无法再提供更多训练历史（已包含最近 $included 个训练日）。请基于现有数据直接给出推荐。"
            } else {
                null
            }
            val userText = AiPrompts.partPlanUser(trainGroups, window, additionalNote)
            val reply = planExecutor.request(
                "aicoach-part-plan", AiPrompts.partPlanSystem(), userText, history, PartPlanReply.serializer(),
            ).getOrElse { return AiCoachResult.Failed(userFacingError(it), retryable = true) }

            if (!reply.needMore) {
                val mapped = mapPlan(reply, trainGroups, sessionsUsed = included, rounds = rounds)
                return if (mapped is AiCoachResult.TodayPlan) {
                    mapped.copy(
                        newMessages = turn(
                            userLabel = planRequestLabel(today, sessionsUsed = included),
                            assistantLabel = planSummary(mapped.parts),
                        )
                    )
                } else {
                    mapped
                }
            }

            val canExpand = !forceFallback &&
                included < historyDays.size &&
                included < MAX_SESSIONS &&
                rounds <= MAX_ROUNDS
            if (canExpand) {
                val want = (reply.wantSessions ?: 1).coerceAtLeast(1)
                included = min(MAX_SESSIONS, max(included + 1, min(historyDays.size, included + want)))
                continue
            }
            if (!forceFallback) {
                // One final attempt telling the model it cannot get more history.
                forceFallback = true
                continue
            }
            return AiCoachResult.Failed(
                "AI 在多次补充训练历史后仍认为数据不足，无法给出建议。请稍后重试或更换模型。",
                retryable = true,
            )
        }
    }

    private fun mapPlan(
        reply: PartPlanReply,
        trainGroups: List<TrainPartGroup>,
        sessionsUsed: Int,
        rounds: Int,
    ): AiCoachResult {
        val ignored = mutableListOf<String>()
        val rawParts = reply.plan.orEmpty()
        if (rawParts.isEmpty()) {
            return AiCoachResult.Failed("AI 未返回可用的训练计划，请重试。", retryable = true)
        }
        val parts = rawParts.mapNotNull { planPart ->
            val group = trainGroups.firstOrNull {
                it.part.partName.normalizeForMatch() == planPart.partName.normalizeForMatch()
            }
            if (group == null) {
                ignored += planPart.partName
                return@mapNotNull null
            }
            val actions = planPart.actions.mapNotNull { actionPlan ->
                val trainAction = group.actions.firstOrNull {
                    it.actionName.normalizeForMatch() == actionPlan.actionName.normalizeForMatch()
                }
                val sets = actionPlan.sets
                if (trainAction == null || sets == null || sets !in 1..12) {
                    ignored += actionPlan.actionName
                    return@mapNotNull null
                }
                RecommendedAction(
                    actionName = trainAction.actionName,
                    sets = sets,
                    reps = actionPlan.reps?.takeIf { it in 1..100 },
                    weightKg = actionPlan.weightKg?.takeIf { it > 0.0 },
                    durationSec = actionPlan.durationSec?.takeIf { it in 1..3600 },
                )
            }
            if (actions.isEmpty()) {
                ignored += planPart.partName
                return@mapNotNull null
            }
            RecommendedPart(
                partName = group.part.partName,
                isPrimary = planPart.isPrimary,
                reason = planPart.reason,
                actions = actions,
            )
        }.distinctBy { it.partName }

        if (parts.isEmpty()) {
            return AiCoachResult.Failed(
                "AI 返回的计划无法匹配到你的动作库（可能给出了不存在的部位/动作），请重试。",
                retryable = true,
            )
        }
        return AiCoachResult.TodayPlan(
            sessionsUsed = sessionsUsed,
            rounds = rounds,
            parts = parts,
            ignoredNames = ignored.distinct(),
        )
    }

    // ------------------------------------------------------------------ Case B

    private suspend fun nextAdvice(
        today: LocalDate,
        allWorkouts: DailyWorkoutMap,
        todayWorkout: DailyWorkout,
        trainGroups: List<TrainPartGroup>,
        history: List<CoachMessage>,
    ): AiCoachResult {
        val todaySummary = HistorySummarizer.summarize(todayWorkout, today)
        if (todaySummary == null || todaySummary.parts.isEmpty()) {
            return AiCoachResult.Failed("无法确定今天已练的部位，请重试。", retryable = true)
        }
        val setsToday = todayWorkout.actions.sumOf { it.trainAction.size }
        // The "current part" = part of the most recent set of today.
        val currentPartName = todayWorkout.actions
            .maxByOrNull { pair -> pair.trainAction.maxOf { it.instant } }
            ?.action?.part?.partName
        if (currentPartName == null) {
            return AiCoachResult.Failed("无法确定当前训练部位，请重试。", retryable = true)
        }
        val currentGroup = trainGroups.firstOrNull {
            it.part.partName.normalizeForMatch() == currentPartName.normalizeForMatch()
        } ?: return AiCoachResult.Failed("当前训练部位不在动作库中，无法继续。", retryable = true)

        // Same-part history: sessions before today that trained currentPartName,
        // restricted to that part only, most recent 5.
        val partOnlyDays = allWorkouts.trainedDate.entries
            .filter { it.key < today && HistorySummarizer.containsPart(it.value, currentPartName) }
            .sortedBy { it.key }
        val partHistory = partOnlyDays.mapNotNull {
            HistorySummarizer.summarizePartOnly(it.value, currentPartName, today)
        }.takeLast(PART_HISTORY_SESSIONS)
        val lastPartDaysAgo = partOnlyDays.lastOrNull()?.let {
            (today.toEpochDays() - it.key.toEpochDays()).toInt()
        }

        val userText = AiPrompts.nextAdviceUser(trainGroups, todaySummary, partHistory, lastPartDaysAgo)
        val reply = planExecutor.request(
            "aicoach-next-advice", AiPrompts.nextAdviceSystem(), userText, history, NextAdviceReply.serializer(),
        ).getOrElse { return AiCoachResult.Failed(userFacingError(it), retryable = true) }

        val ignored = mutableListOf<String>()

        // Validate/maybe-fix the action name against the current part.
        val actionName = reply.actionName?.let { candidate ->
            currentGroup.actions.firstOrNull {
                it.actionName.normalizeForMatch() == candidate.normalizeForMatch()
            }?.actionName
                ?: run {
                    ignored += candidate
                    null
                }
        }

        val kind = when (reply.kind) {
            AdviceKindReply.CONTINUE_CURRENT -> AdviceKind.CONTINUE_CURRENT
            AdviceKindReply.SWITCH_ACTION -> AdviceKind.SWITCH_ACTION
            AdviceKindReply.FINISH_PART -> AdviceKind.FINISH_PART
            AdviceKindReply.FINISH_DAY -> AdviceKind.FINISH_DAY
        }

        if (kind == AdviceKind.SWITCH_ACTION && actionName == null) {
            return AiCoachResult.Failed(
                "AI 建议更换动作，但给出的动作不在你的动作库中，请重试。",
                retryable = true,
            )
        }
        val nextPartName = reply.nextPartName?.let { candidate ->
            trainGroups.firstOrNull {
                it.part.partName.normalizeForMatch() == candidate.normalizeForMatch()
            }?.part?.partName
                ?: run {
                    ignored += candidate
                    null
                }
        }

        val sets = when {
            reply.sets != null && reply.sets in 1..12 -> reply.sets
            kind == AdviceKind.FINISH_DAY || kind == AdviceKind.FINISH_PART -> 0
            else -> 1
        }
        val advice = Advice(
            kind = kind,
            actionName = actionName,
            sets = sets,
            reps = reply.reps?.takeIf { it in 1..100 },
            weightKg = reply.weightKg?.takeIf { it > 0.0 },
            durationSec = reply.durationSec?.takeIf { it in 1..3600 },
            nextPartName = nextPartName,
            reason = reply.reason,
        )
        return AiCoachResult.NextAdvice(
            advice = advice,
            ignoredNames = ignored.distinct(),
            newMessages = turn(
                userLabel = adviceRequestLabel(today, setsToday, currentPartName),
                assistantLabel = adviceSummary(advice),
            ),
        )
    }

    // ------------------------------------------------------------- turn helpers

    /** New conversation turn (compact user label + assistant summary) for the caller to append. */
    private fun turn(userLabel: String, assistantLabel: String): List<CoachMessage> = listOf(
        CoachMessage(fromUser = true, text = userLabel),
        CoachMessage(fromUser = false, text = assistantLabel),
    )

    private fun planRequestLabel(today: LocalDate, sessionsUsed: Int): String =
        "[$today][Case A] 今日尚无锻炼记录；基于最近 $sessionsUsed 个训练日请求推荐"

    private fun adviceRequestLabel(today: LocalDate, setsToday: Int, currentPartName: String): String =
        "[$today][Case B] 今日已练 $setsToday 组，当前部位：$currentPartName；请求下一步建议"

    private fun planSummary(parts: List<RecommendedPart>): String = parts.joinToString("；") { part ->
        val primary = if (part.isPrimary) "（主推）" else ""
        part.partName + primary + "：" + part.actions.joinToString("、") { actionSummary(it) }
    }

    private fun actionSummary(action: RecommendedAction): String = buildList {
        add(action.actionName)
        add("${action.sets}组")
        action.reps?.let { add("×$it") }
        action.weightKg?.let { add("@${formatNumber(it)}kg") }
        action.durationSec?.let { add("${it}秒") }
    }.joinToString(" ")

    private fun adviceSummary(advice: Advice): String {
        val head = when (advice.kind) {
            AdviceKind.CONTINUE_CURRENT -> "继续${advice.actionName ?: "当前动作"}"
            AdviceKind.SWITCH_ACTION -> "换练${advice.actionName ?: "?（同部位其他动作）"}"
            AdviceKind.FINISH_PART -> "该部位今天已够"
            AdviceKind.FINISH_DAY -> "今天总量已够，建议收工"
        }
        val params = buildList {
            if (advice.sets > 0) add("${advice.sets}组")
            advice.reps?.let { add("×$it") }
            advice.weightKg?.let { add("@${formatNumber(it)}kg") }
            advice.durationSec?.let { add("每组${it}秒") }
            advice.nextPartName?.let { add("下一步建议部位：$it") }
        }.joinToString(" ")
        val reason = advice.reason?.let { "；理由：$it" }.orEmpty()
        return listOf(head, params).filter { it.isNotBlank() }.joinToString(" ") + reason
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun userFacingError(cause: Throwable): String {
        val message = cause.message ?: cause.javaClass.simpleName
        val lower = message.lowercase()
        return when {
            "401" in message || "403" in message || "api key" in lower || "unauthorized" in lower ||
                "authentication" in lower -> "API Key 无效或无权限，请检查 AI 设置中的配置。"
            "timeout" in lower || "timed out" in lower || "connect" in lower ||
                "network" in lower || "socket" in lower -> "网络连接失败或超时，请检查网络后重试。"
            "429" in message || "rate limit" in lower -> "请求过于频繁（限流），请稍后重试。"
            else -> "AI 请求失败：$message"
        }
    }

    private companion object {
        const val INITIAL_SESSIONS = 10
        const val MAX_SESSIONS = 20
        const val MAX_ROUNDS = 2
        const val PART_HISTORY_SESSIONS = 5

        /** 5 rounds = 10 messages carried into the next request. */
        const val HISTORY_MESSAGES = 10
    }
}

private fun todayLocalDate(): LocalDate =
    kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
