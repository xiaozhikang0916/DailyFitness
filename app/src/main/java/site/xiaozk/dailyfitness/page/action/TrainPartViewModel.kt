package site.xiaozk.dailyfitness.page.action

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import site.xiaozk.dailyfitness.repository.ITrainActionRepository
import site.xiaozk.dailyfitness.repository.model.TrainPartPage
import javax.inject.Inject

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
@HiltViewModel
class TrainPartViewModel @Inject constructor(
    private val trainRepo: ITrainActionRepository,
) : ViewModel() {
    val trainParts: Flow<TrainPartPage> = trainRepo.getAllActions()
}