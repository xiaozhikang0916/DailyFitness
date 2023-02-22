package site.xiaozk.dailyfitness.page.training

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import site.xiaozk.dailyfitness.page.home.TrainedDayDateProvider
import site.xiaozk.dailyfitness.repository.model.TrainingDayData

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/25
 */

@Preview
@Composable
fun PreviewTrainingDayDetail(@PreviewParameter(provider = TrainedDayDateProvider::class) data: TrainingDayData) {
    TrainingDayDetail(data = data)
}