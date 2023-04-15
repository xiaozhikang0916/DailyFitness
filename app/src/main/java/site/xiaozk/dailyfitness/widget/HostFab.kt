package site.xiaozk.dailyfitness.widget

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.nav.AddDailyBodyDetailNavItem
import site.xiaozk.dailyfitness.nav.IScaffoldState
import site.xiaozk.dailyfitness.nav.Route
import site.xiaozk.dailyfitness.nav.TrainPartGraph
import site.xiaozk.dailyfitness.nav.TrainingDayGroup
import kotlin.math.max
import kotlin.math.min

/**
 * @author: xiaozhikang
 * @create: 2023/3/30
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostFab(scaffoldState: IScaffoldState?, topAppBarState: TopAppBarState? = null, onRoute: (Route) -> Unit) {
    var showFabMenu by remember {
        mutableStateOf(false)
    }
    if (scaffoldState?.showFab == true) {
        Box {
            FloatingActionButtonShowHide(
                onClick = {
                    showFabMenu = !showFabMenu
                },
                topAppBarState = topAppBarState
            ) {
                Image(
                    painter = rememberVectorPainter(image = Icons.Default.Add),
                    contentDescription = null
                )
            }
            val expandedStates = remember { MutableTransitionState(false) }

            expandedStates.targetState = showFabMenu
            if (showFabMenu) {

                if (expandedStates.currentState || expandedStates.targetState) {
                    val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
                    val density = LocalDensity.current
                    val popupPositionProvider = DropdownMenuPositionProvider(
                        DpOffset(x = 0.dp, y = (24).dp),
                        density
                    ) { parentBounds, menuBounds ->
                        transformOriginState.value = calculateTransformOrigin(parentBounds, menuBounds)
                    }
                    Popup(
                        onDismissRequest = { showFabMenu = false },
                        popupPositionProvider = popupPositionProvider,
                        properties = PopupProperties(focusable = true),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            FloatingActionListButton(
                                icon = Icons.Default.Add,
                                text = stringResource(R.string.fab_action_add_workout),
                            ) {
                                onRoute(Route(TrainingDayGroup.TrainDayAddActionNavItem.route))
                                showFabMenu = false
                            }
                            FloatingActionListButton(text = stringResource(R.string.fab_action_add_body_data)) {
                                onRoute(Route(AddDailyBodyDetailNavItem.route))
                                showFabMenu = false
                            }
                            FloatingActionListButton(text = stringResource(R.string.fab_action_add_train_group)) {
                                onRoute(Route(TrainPartGraph.AddTrainPartNavItem.getRoute()))
                                showFabMenu = false
                            }
                            FloatingActionListButton(text = stringResource(R.string.fab_action_add_train_action)) {
                                onRoute(Route(TrainPartGraph.AddTrainActionNavItem.getRoute()))
                                showFabMenu = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingActionListButton(
    text: String,
    icon: ImageVector = Icons.Default.Add,
    iconDesc: String? = null,
    onClick: () -> Unit,
) {

    SmallFloatingActionButton(onClick = onClick) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = text)
            Icon(
                painter = rememberVectorPainter(image = icon),
                contentDescription = iconDesc,
            )
        }
    }
}

@Immutable
internal data class DropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> },
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { 48.dp.roundToPx() }
        // The content offset specified using the dropdown offset parameter.
        val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        // Compute horizontal position.
        val toRight = anchorBounds.left + contentOffsetX
        val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
        val toDisplayRight = windowSize.width - popupContentSize.width
        val toDisplayLeft = 0
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(
                toRight,
                toLeft,
                // If the anchor gets outside of the window on the left, we want to position
                // toDisplayLeft for proximity to the anchor. Otherwise, toDisplayRight.
                if (anchorBounds.left >= 0) toDisplayRight else toDisplayLeft
            )
        } else {
            sequenceOf(
                toLeft,
                toRight,
                // If the anchor gets outside of the window on the right, we want to position
                // toDisplayRight for proximity to the anchor. Otherwise, toDisplayLeft.
                if (anchorBounds.right <= windowSize.width) toDisplayLeft else toDisplayRight
            )
        }.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: toLeft

        // Compute vertical position.
        val toBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
        val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
        val toCenter = anchorBounds.top - popupContentSize.height / 2
        val toDisplayBottom = windowSize.height - popupContentSize.height - verticalMargin
        val y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull {
            it >= verticalMargin &&
                it + popupContentSize.height <= windowSize.height - verticalMargin
        } ?: toTop

        onPositionCalculated(
            anchorBounds,
            IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
        )
        return IntOffset(x, y)
    }
}

internal fun calculateTransformOrigin(
    parentBounds: IntRect,
    menuBounds: IntRect,
): TransformOrigin {
    val pivotX = when {
        menuBounds.left >= parentBounds.right -> 0f
        menuBounds.right <= parentBounds.left -> 1f
        menuBounds.width == 0 -> 0f
        else -> {
            val intersectionCenter =
                (
                    max(parentBounds.left, menuBounds.left) +
                        min(parentBounds.right, menuBounds.right)
                    ) / 2
            (intersectionCenter - menuBounds.left).toFloat() / menuBounds.width
        }
    }
    val pivotY = when {
        menuBounds.top >= parentBounds.bottom -> 0f
        menuBounds.bottom <= parentBounds.top -> 1f
        menuBounds.height == 0 -> 0f
        else -> {
            val intersectionCenter =
                (
                    max(parentBounds.top, menuBounds.top) +
                        min(parentBounds.bottom, menuBounds.bottom)
                    ) / 2
            (intersectionCenter - menuBounds.top).toFloat() / menuBounds.height
        }
    }
    return TransformOrigin(pivotX, pivotY)
}