package site.xiaozk.dailyfitness.page.action

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
@HiltViewModel(assistedFactory = TrainPartViewModel.Factory::class)
class TrainPartViewModel @AssistedInject constructor(
    private val trainRepo: ITrainActionRepository,
    @Assisted("partId") private val partId: Int,
    @Assisted("actionId") private val actionId: Int,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("partId") partId: Int,
            @Assisted("actionId") actionId: Int,
        ): TrainPartViewModel
    }

    val homeTrainPartStatic: Flow<HomeTrainPartPage> = trainRepo.getAllTrainPartStatics()

    val trainPartStatic: Flow<TrainPartStaticPage?> by lazy {
        trainRepo.getTrainPartStatic(partId = partId)
    }
    val trainActionStatic: Flow<TrainActionStaticPage?> by lazy {
        trainRepo.getTrainActionStatic(
            actionId = actionId
        )
    }
}