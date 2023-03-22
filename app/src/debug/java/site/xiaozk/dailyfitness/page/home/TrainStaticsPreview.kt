package site.xiaozk.dailyfitness.page.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import site.xiaozk.dailyfitness.page.action.HomeTrainPartHeadCard
import site.xiaozk.dailyfitness.page.action.TrainActionPage
import site.xiaozk.dailyfitness.page.action.TrainPartPage
import site.xiaozk.dailyfitness.page.action.TrainStaticPage
import site.xiaozk.dailyfitness.providers.HomeTrainPartPageProvider
import site.xiaozk.dailyfitness.providers.TrainActionStaticPageProvider
import site.xiaozk.dailyfitness.providers.TrainPartStaticPageProvider
import site.xiaozk.dailyfitness.repository.model.HomeTrainPartPage
import site.xiaozk.dailyfitness.repository.model.TrainActionStaticPage
import site.xiaozk.dailyfitness.repository.model.TrainPartStaticPage
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

@Preview
@Composable
fun TrainPartStaticPagePreview(@PreviewParameter(TrainPartStaticPageProvider::class) trainPartStaticPage: TrainPartStaticPage) {
    DailyFitnessTheme {
        TrainPartPage(trainPartStaticPage)
    }
}

@Preview
@Composable
fun TrainActionStaticPagePreview(@PreviewParameter(TrainActionStaticPageProvider::class) trainActionStaticPage: TrainActionStaticPage) {
    DailyFitnessTheme {
        TrainActionPage(actionStaticPage = trainActionStaticPage)
    }
}