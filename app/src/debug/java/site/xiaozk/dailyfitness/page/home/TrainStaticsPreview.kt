package site.xiaozk.dailyfitness.page.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import site.xiaozk.dailyfitness.page.action.HomeTrainPartHeadCard
import site.xiaozk.dailyfitness.page.action.TrainStaticPage
import site.xiaozk.dailyfitness.providers.HomeTrainPartPageProvider
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.theme.DailyFitnessTheme

/**
 * @author: xiaozhikang
 * @create: 2023/3/20
 */
@Preview
@Composable
fun HomeTrainPartPagePreview(@PreviewParameter(HomeTrainPartPageProvider::class) homeTrainPartPage: HomeTrainPartPage) {
    DailyFitnessTheme {
        TrainStaticPage(homeTrainPartPage) {}
    }
}

@Preview
@Composable
fun HomeTrainPartCardPreview(@PreviewParameter(HomeTrainPartPageProvider::class) homeTrainPartPage: HomeTrainPartPage) {
    DailyFitnessTheme {
        HomeTrainPartHeadCard(homeTrainPartPage)
    }
}