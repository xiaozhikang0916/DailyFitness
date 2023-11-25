package site.xiaozk.dailyfitness.nav

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import androidx.navigation.navigation
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.nav.AppHomeRootNav.AppHomePage.TrainPartNavItem.homeGraph
import site.xiaozk.dailyfitness.page.action.DeleteTrainActionDialog
import site.xiaozk.dailyfitness.page.action.TrainActionPage
import site.xiaozk.dailyfitness.page.action.TrainPartPage
import site.xiaozk.dailyfitness.page.action.TrainStaticPage
import site.xiaozk.dailyfitness.page.action.parts.AddTrainActionPage
import site.xiaozk.dailyfitness.page.action.parts.AddTrainPartPage
import site.xiaozk.dailyfitness.page.body.BodyDetailPage
import site.xiaozk.dailyfitness.page.body.add.AddDailyBodyDetail
import site.xiaozk.dailyfitness.page.training.HomeWorkoutPage
import site.xiaozk.dailyfitness.page.training.TrainingDayDetailPage
import site.xiaozk.dailyfitness.page.training.WorkoutMonthlyPage
import site.xiaozk.dailyfitness.page.training.add.AddDailyWorkoutAction
import site.xiaozk.dailyfitness.page.training.add.DeleteDailyWorkout
import site.xiaozk.dailyfitness.repository.model.TrainAction
import site.xiaozk.dailyfitness.repository.model.TrainActionWithPart
import site.xiaozk.dailyfitness.repository.model.TrainPart
import site.xiaozk.dailyfitness.repository.model.YearMonth

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/2/26
 */
private val json = Json {
    ignoreUnknownKeys = true
}

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
        navigation(
            route = AppRootNav.route,
            startDestination = startWith
        ) {
            homeGraph()
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
                return context.getString(R.string.bottom_nav_title_home)
            }
        }

        object BodyDetailNavItem : AppHomePage() {
            override val route = "/home/body"
            override val icon = Icons.Default.AccountBox
            override fun getName(context: Context): String {
                return context.getString(R.string.bottom_nav_title_body)
            }
        }

        object TrainPartNavItem : AppHomePage() {
            override val route = "/home/train_part"
            override val icon = Icons.Default.List
            override fun getName(context: Context): String {
                return context.getString(R.string.bottom_nav_title_train)
            }
        }

        companion object {
            fun all(): List<AppHomePage> =
                listOf(TrainingHomeNavItem, BodyDetailNavItem, TrainPartNavItem)
        }

        fun NavGraphBuilder.homeGraph() {
            navigation(route = AppHomeRootNav.route, startDestination = startWith) {
                composable(TrainingHomeNavItem.route) {
                    HomeWorkoutPage()
                }

                composable(BodyDetailNavItem.route) {
                    BodyDetailPage()
                }

                composable(TrainPartNavItem.route) {
                    TrainStaticPage()
                }
            }
        }
    }
}

object WorkoutStaticGroup {
    object WorkoutMonthNavItem : IAppNavItem {
        override val route: String
            get() = "workout/month?date={date}"

        fun getRoute(month: YearMonth = YearMonth.now()): String {
            return "workout/month?date=${parseArgument(month)}"
        }

        fun parseArgument(month: YearMonth): String {
            return Json.encodeToString(month)
        }

        fun fromArgument(arg: String): YearMonth {
            return try {
                Json.decodeFromString<YearMonth>(arg)
            } catch (e: Exception) {
                YearMonth.now()
            }
        }
    }

    fun NavGraphBuilder.workoutStaticGraph() {
        composable(
            WorkoutMonthNavItem.route,
            arguments = listOf(navArgument("year_month") {
                type = NavType.StringType
                nullable = true
            })
        ) {
            WorkoutMonthlyPage()
        }
    }
}

object TrainingDayGroup {
    object TrainDayNavItem : IAppNavItem {
        override val route: String
            get() = "train_day/day?date={date}"

        fun getRoute(date: LocalDate): String {
            return "train_day/day?date=${Json.encodeToString(date)}"
        }

        fun fromArgument(arg: String): LocalDate {
            return try {
                Json.decodeFromString<LocalDate>(arg)
            } catch (e: Exception) {
                Log.e("TrainDayNavItem", "parse $arg to local date failed", e)
                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            }
        }
    }

    object TrainDayAddActionNavItem : IAppNavItem {
        override val route: String
            get() = "train_day/new_action"
    }

    object DeleteWorkoutNavItem : IAppNavItem {
        override val route: String
            get() = "train_day/delete_workout?workoutId={workoutId}"

        fun getRoute(workoutId: Int): String {
            return "train_day/delete_workout?workoutId=${workoutId}"
        }
    }

    fun NavGraphBuilder.trainingDayGraph() {
        composable(
            TrainDayNavItem.route,
            arguments = listOf(navArgument("date") { nullable = true })
        ) {
            TrainingDayDetailPage()
        }
        composable(TrainDayAddActionNavItem.route) {
            AddDailyWorkoutAction()
        }
        dialog(
            route = DeleteWorkoutNavItem.route,
            arguments = listOf(navArgument("workoutId") {
                nullable = false
                type = NavType.IntType
                defaultValue = -1
            })
        ) {
            DeleteDailyWorkout()
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
            get() = "train_part/add?partId={partId}"

        fun getRoute(part: TrainPart? = null): String {
            return "train_part/add?partId=${part?.id ?: -1}"
        }
    }

    object TrainPartDetailNavItem : IAppNavItem {

        override val route: String
            get() = "train_part/detail?partId={partId}"

        fun getRoute(part: TrainPart): String {
            return "train_part/detail?partId=${part.id}"
        }
    }

    object TrainActionDetailNavItem : IAppNavItem {

        override val route: String
            get() = "train_part/action?actionId={actionId}"

        fun getRoute(action: TrainAction): String {
            return "train_part/action?actionId=${action.id}"
        }
    }

    object AddTrainActionNavItem : IAppNavItem {

        override val route: String
            get() = "train_part/add_action?partId={partId}&actionId={actionId}"

        fun getRoute(part: TrainPart? = null, action: TrainActionWithPart? = null): String {
            return "train_part/add_action?partId=${part?.id ?: 0}&actionId=${action?.id ?: 0}"
        }

        fun getRoute(action: TrainAction? = null): String {
            return "train_part/add_action?partId=${action?.partId ?: 0}&actionId=${action?.id ?: 0}"
        }
    }

    object DeleteTrainActionNavItem : IAppNavItem {

        override val route: String
            get() = "train_part/delete_action?actionId={actionId}"

        fun getRoute(action: TrainAction? = null): String {
            return "train_part/delete_action?actionId=${action?.id ?: 0}"
        }
    }

    fun NavGraphBuilder.trainPartGraph() {

        dialog(
            AddTrainPartNavItem.route,
            arguments = listOf(navArgument("partId") {
                defaultValue = 0
                type = NavType.IntType
            })
        ) {
            AddTrainPartPage()
        }
        composable(
            TrainPartDetailNavItem.route,
            arguments = listOf(navArgument("partId") {
                nullable = false
                type = NavType.IntType
            })
        ) {
            TrainPartPage()
        }
        composable(
            TrainActionDetailNavItem.route,
            arguments = listOf(navArgument("actionId") {
                nullable = false
                type = NavType.IntType
            })
        ) {
            TrainActionPage()
        }
        composable(
            AddTrainActionNavItem.route,
            arguments = listOf(
                navArgument("partId") {
                    nullable = false
                    type = NavType.IntType
                },
                navArgument("actionId") {
                    defaultValue = 0
                    type = NavType.IntType
                }
            )
        ) {
            AddTrainActionPage()
        }
        dialog(
            DeleteTrainActionNavItem.route,
            arguments = listOf(
                navArgument("actionId") {
                    nullable = false
                    type = NavType.IntType
                }
            )
        ) {
            DeleteTrainActionDialog()
        }
    }
}

