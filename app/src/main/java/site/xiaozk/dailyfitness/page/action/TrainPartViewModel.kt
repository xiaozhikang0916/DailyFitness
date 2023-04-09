package site.xiaozk.dailyfitness.page.action

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
@HiltViewModel
class TrainPartViewModel @Inject constructor(
    private val trainRepo: ITrainActionRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val homeTrainPartStatic: Flow<HomeTrainPartPage> = trainRepo.getAllTrainPartStatics()

    val partId: Int
        get() = savedStateHandle["partId"] ?: -1

    val actionId: Int
        get() = savedStateHandle["actionId"] ?: -1
    val trainPartStatic: Flow<TrainPartStaticPage?> by lazy {
        trainRepo.getTrainPartStatic(partId = partId)
    }
    val trainActionStatic: Flow<TrainActionStaticPage?> by lazy {
        trainRepo.getTrainActionStatic(
            actionId = actionId
        )
    }
}