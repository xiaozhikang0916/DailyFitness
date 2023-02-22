package site.xiaozk.dailyfitness.nav

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/3
 */
interface INavController {
    fun popBackStack(): Boolean
    fun navigate(
        route: String,
        navOptions: NavOptions? = null,
        navigatorExtras: Navigator.Extras? = null,
    )
    val navController: NavController
}

class NavControllerDispatcher(val delegate: NavController) : INavController {
    var parentDelegate: INavController? = null

    override fun popBackStack(): Boolean {
        return delegate.popBackStack() || (parentDelegate?.popBackStack() ?: false)
    }

    override fun navigate(route: String, navOptions: NavOptions?, navigatorExtras: Navigator.Extras?) {
        Log.d("NavControllerDispatcher", "navigate to $route")
        try {
            delegate.navigate(route, navOptions, navigatorExtras)
            Log.d("NavControllerDispatcher", "dest in self found, navigating to $route")
        } catch (e: IllegalArgumentException) {
            Log.d("NavControllerDispatcher", "dest in self not found, re-dispatch to parent")
            parentDelegate?.navigate(route, navOptions, navigatorExtras)
        }
    }

    override val navController: NavController
        get() = delegate
}

fun NavController.wrapController(): INavController = NavControllerDispatcher(this)

@Composable
fun NavController.provides(): ProvidedValue<INavController?> {
    val parent = LocalNavController.current
    return LocalNavController provides (this.wrapController() as NavControllerDispatcher).also {
        it.parentDelegate = parent
    }
}

val LocalNavController = compositionLocalOf<INavController?> { null }