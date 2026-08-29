package site.xiaozk.dailyfitness.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.datetime.serializers.YearMonthIso8601Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import site.xiaozk.dailyfitness.R

/**
 * Navigation 3 (androidx.navigation3) type-safe routes.
 *
 * Replaces the string-based routes of the previous Navigation Compose 2.x implementation:
 * - args become typed properties on the route class (no more URL-encoding via Json)
 * - navigation is done with route instances (navBackStack.add(route) / removeLastOrNull())
 *
 * All routes implement [NavKey] and are [Serializable] so the back stack can be
 * saved/restored across process death.
 */

// ---- No-arg routes ----
@Serializable
object HomeTraining : NavKey

@Serializable
object HomeBody : NavKey

@Serializable
object HomeTrainPart : NavKey

@Serializable
object AddWorkoutAction : NavKey

@Serializable
object AddBodyDetail : NavKey

// ---- Routes with arguments ----
@Serializable
data class WorkoutMonth(
    @Serializable(with = YearMonthIso8601Serializer::class)
    val date: YearMonth,
) : NavKey

@Serializable
data class TrainDay(
    @Serializable(with = LocalDateIso8601Serializer::class)
    val date: LocalDate,
) : NavKey

@Serializable
data class TrainPartDetail(
    val partId: Int,
) : NavKey

@Serializable
data class TrainActionDetail(
    val actionId: Int,
) : NavKey

@Serializable
data class AddTrainAction(
    val partId: Int,
    val actionId: Int,
) : NavKey

/**
 * Serializers module registering every [NavKey] subtype for open polymorphism.
 *
 * Not strictly required on Android (the reflection-based `rememberNavBackStack()`
 * overload handles restoration), but needed if we switch to the explicit
 * `SavedStateConfiguration` path (e.g. if release + R8 breaks reflection restore).
 *
 * Note: dialogs are no longer nav destinations (they are local UI state), so only
 * screen routes are registered here.
 */
val NavKeySerializersModule: SerializersModule = SerializersModule {
    polymorphic(NavKey::class) {
        subclass(HomeTraining::class, HomeTraining.serializer())
        subclass(HomeBody::class, HomeBody.serializer())
        subclass(HomeTrainPart::class, HomeTrainPart.serializer())
        subclass(AddWorkoutAction::class, AddWorkoutAction.serializer())
        subclass(AddBodyDetail::class, AddBodyDetail.serializer())
        subclass(WorkoutMonth::class, WorkoutMonth.serializer())
        subclass(TrainDay::class, TrainDay.serializer())
        subclass(TrainPartDetail::class, TrainPartDetail.serializer())
        subclass(TrainActionDetail::class, TrainActionDetail.serializer())
        subclass(AddTrainAction::class, AddTrainAction.serializer())
    }
}

/**
 * Bottom navigation tabs, replacing the old `AppHomePage` string-route hierarchy.
 * Used by [site.xiaozk.dailyfitness.widget.AppBottomBar].
 */
enum class HomeTab(
    val route: NavKey,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    TRAINING(HomeTraining, Icons.Default.Home, R.string.bottom_nav_title_home),
    BODY(HomeBody, Icons.Default.AccountBox, R.string.bottom_nav_title_body),
    TRAIN_PART(HomeTrainPart, Icons.AutoMirrored.Filled.List, R.string.bottom_nav_title_train),
}
