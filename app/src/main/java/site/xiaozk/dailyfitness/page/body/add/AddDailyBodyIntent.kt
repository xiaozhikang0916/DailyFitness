package site.xiaozk.dailyfitness.page.body.add

import android.util.Log
import site.xiaozk.dailyfitness.base.ActionStatus
import site.xiaozk.dailyfitness.base.IIntent
import site.xiaozk.dailyfitness.base.IntentResult
import site.xiaozk.dailyfitness.repository.IPersonDailyRepository
import site.xiaozk.dailyfitness.repository.IUserRepository
import site.xiaozk.dailyfitness.repository.model.BodyField
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/27
 */

sealed interface IDailyBodyIntent : IIntent

typealias AddDailyBodyResult = IntentResult<AddDailyBodyState, IDailyBodyIntent>

class InputIntent(val num: String, val field: BodyField) : IDailyBodyIntent

object SubmitBodyIntent : IDailyBodyIntent

class SubmitDoneIntent(val status: ActionStatus) : IDailyBodyIntent

class AddDailyBodyReducer @Inject constructor(
    private val bodyRepo: IPersonDailyRepository,
    private val userRepo: IUserRepository,
) {
    fun reduce(state: AddDailyBodyState, intent: IDailyBodyIntent): AddDailyBodyResult {
        return when (intent) {
            is InputIntent -> AddDailyBodyResult(state = state.copy(bodyField = state.bodyField.copy {
                it[intent.field] = intent.num
            }))

            SubmitBodyIntent -> AddDailyBodyResult(state = state) {
                try {
                    bodyRepo.addPersonDailyData(
                        userRepo.getCurrentUser(),
                        state.toDailyBodyData()
                    )
                    emit(SubmitDoneIntent(ActionStatus.Done))
                } catch (e: Exception) {
                    Log.e("AddDailyBody", "Add daily body detail failed", e)
                    emit(SubmitDoneIntent(ActionStatus.Failed(e)))
                }
            }

            is SubmitDoneIntent -> AddDailyBodyResult(state = state.copy(submitStatus = intent.status))
        }
    }
}