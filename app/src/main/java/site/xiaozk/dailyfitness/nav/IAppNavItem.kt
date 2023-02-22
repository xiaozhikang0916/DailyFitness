package site.xiaozk.dailyfitness.nav

import android.content.Context
import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import site.xiaozk.dailyfitness.AppHome
import site.xiaozk.dailyfitness.page.action.TrainPartPage
import site.xiaozk.dailyfitness.page.action.parts.AddTrainActionPage
import site.xiaozk.dailyfitness.page.action.parts.AddTrainPartPage
import site.xiaozk.dailyfitness.page.action.parts.TrainPartDetail
import site.xiaozk.dailyfitness.page.body.BodyDetailPage
import site.xiaozk.dailyfitness.page.body.add.AddDailyBodyDetail
import site.xiaozk.dailyfitness.page.training.TrainingDayDetailPage
import site.xiaozk.dailyfitness.page.training.TrainingHome
import site.xiaozk.dailyfitness.page.training.add.AddDailyTrainAction
import site.xiaozk.dailyfitness.repository.model.TrainPart
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
sealed interface IAppNavItem {
    val route: String
}

sealed interface IAppNavGraphItem : IAppNavItem {
    val startWith: String
}

object AppRootNav : IAppNavGraphItem {
    override val route: String
        get() = "/app"
    override val startWith: String
        get() = AppHomeRootNav.route

    fun NavGraphBuilder.appRootGraph() {
        composable(AppRootNav.route) {
            AppHome(bottomNavController = rememberNavController())
        }
    }
}

object AppHomeRootNav : IAppNavGraphItem {
    override val route: String
        get() = "/home"

    override val startWith: String
        get() = AppHomePage.TrainingHomeNavItem.route

    sealed class AppHomePage : IAppNavItem {

        abstract val icon: ImageVector

        abstract fun getName(context: Context): String

        object TrainingHomeNavItem : AppHomePage() {
            override val route = "/home/training"
            override val icon = Icons.Default.Home
            override fun getName(context: Context): String {
                return "Home"
            }
        }

        object BodyDetailNavItem : AppHomePage() {
            override val route = "/home/body"
            override val icon = Icons.Default.AccountBox
            override fun getName(context: Context): String {
                return "Body"
            }
        }

        object TrainPartNavItem : AppHomePage() {
            override val route = "/home/train_part"
            override val icon = Icons.Default.List
            override fun getName(context: Context): String {
                return "Train"
            }
        }

        companion object {
            fun all(): List<AppHomePage> = listOf(TrainingHomeNavItem, BodyDetailNavItem, TrainPartNavItem)
        }

        fun NavGraphBuilder.homeGraph() {
            navigation(route = AppHomeRootNav.route, startDestination = startWith) {
                composable(TrainingHomeNavItem.route) {
                    TrainingHome()
                }

                composable(BodyDetailNavItem.route) {
                    BodyDetailPage()
                }

                composable(TrainPartNavItem.route) {
                    TrainPartPage()
                }
            }
        }
    }
}

object TrainingDayGroup {
    object TrainDayNavItem : IAppNavItem {
        override val route: String
            get() = "train_day/day?date={date}"

        private val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault())
        fun getRoute(date: LocalDate): String {
            return "train_day/day?date=${dateFormat.format(date)}"
        }

        fun fromArgument(arg: String): LocalDate {
            return LocalDate.from(dateFormat.parse(arg))
        }

        fun fromArgument(argument: Bundle?): LocalDate {
            return argument?.getString("date")?.let(this::fromArgument) ?: LocalDate.now()
        }
    }

    object TrainDayAddActionNavItem : IAppNavItem {
        override val route: String
            get() = "train_day/new_action"
    }

    fun NavGraphBuilder.trainingDayGraph() {
        composable(TrainDayNavItem.route, arguments = listOf(navArgument("date") { nullable = true })) {
            TrainingDayDetailPage(date = TrainDayNavItem.fromArgument(it.arguments))
        }
        composable(TrainDayAddActionNavItem.route) {
            AddDailyTrainAction()
        }
    }
}


object AddDailyBodyDetailNavItem : IAppNavItem {
    override val route: String
        get() = "body/add"

    fun NavGraphBuilder.addDailyBodyDetailNav() {
        composable(AddDailyBodyDetailNavItem.route) {
            AddDailyBodyDetail()
        }
    }
}

object TrainPartGraph {

    object AddTrainPartNavItem : IAppNavItem {
        override val route: String
            get() = "train_part/add"
    }

    object TrainPartDetailNavItem : IAppNavItem {

        override val route: String
            get() = "train_part/detail?part={part}"

        fun getRoute(part: TrainPart): String {
            return "train_part/detail?part=${part.id}"
        }

        fun fromArgument(arg: String): Int {
            return arg.toIntOrNull() ?: 0
        }

        fun fromArgument(argument: Bundle?): Int {
            return argument?.getString("part")?.let(::fromArgument) ?: 0
        }
    }

    object AddTrainActionNavItem : IAppNavItem {

        override val route: String
            get() = "train_part/add_action?part={part}"

        fun getRoute(part: TrainPart): String {
            return "train_part/add_action?part=${part.id}"
        }

        fun fromArgument(arg: String): Int {
            return arg.toIntOrNull() ?: 0
        }

        fun fromArgument(argument: Bundle?): Int {
            return argument?.getString("part")?.let(::fromArgument) ?: 0
        }
    }

    fun NavGraphBuilder.trainPartGraph() {

        composable(AddTrainPartNavItem.route) {
            AddTrainPartPage()
        }
        composable(
            TrainPartDetailNavItem.route,
            arguments = listOf(navArgument("part") { nullable = false })
        ) {
            TrainPartDetail(
                trainPartId = TrainPartDetailNavItem.fromArgument(it.arguments),
            )
        }
        composable(
            AddTrainActionNavItem.route,
            arguments = listOf(navArgument("part") { nullable = false })
        ) {
            AddTrainActionPage(
                partId = AddTrainActionNavItem.fromArgument(it.arguments),
            )
        }
    }
}

