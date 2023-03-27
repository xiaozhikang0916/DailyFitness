package site.xiaozk.dailyfitness.nav

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * @author: xiaozhikang
 * @create: 2023/3/26
 */

data class TopAction(
    val displayType: ITopDisplayType,
    val actionType: ITopActionType,
    val valid: Boolean = true,
) {
    companion object {
        fun iconRouteAction(icon: ImageVector, actionDesc: String? = null, route: Route, valid: Boolean = true) =
            TopAction(IconType(icon, actionDesc), RouteAction(route), valid)

        fun textPageAction(text: String, type: PageHandleType, valid: Boolean = true) =
            TopAction(TextButtonType(text), PageHandleAction(type), valid)
    }
}

sealed interface ITopDisplayType
data class IconType(val icon: ImageVector, val actionDesc: String? = null) : ITopDisplayType
data class TextButtonType(val text: String) : ITopDisplayType

sealed interface ITopActionType
data class RouteAction(val route: Route) : ITopActionType
data class PageHandleAction(val type: PageHandleType) : ITopActionType

@JvmInline
value class Route(val route: String) {
    val isBack: Boolean
        get() = route == "back"

    companion object {
        val BACK = Route("back")
    }
}

enum class PageHandleType {
    SAVE
}