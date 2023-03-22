package site.xiaozk.dailyfitness.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @author: xiaozhikang
 * @create: 2023/3/22
 */

@Composable
fun SmallChip(
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    labelTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
    minHeight: Dp = 24.dp,
    paddingValues: PaddingValues = PaddingValues(SmallChipPadding),
) {

    Surface(
        modifier = modifier,
        shape = ShapeDefaults.ExtraSmall,
        color = Color.Transparent,
        border = BorderStroke(color = MaterialTheme.colorScheme.outline, width = 1.dp),
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides labelTextStyle,
            LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            Row(
                Modifier
                    .defaultMinSize(minHeight = minHeight)
                    .padding(paddingValues),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(SmallChipPadding))
                label()
                Spacer(Modifier.width(SmallChipPadding))
            }
        }
    }
}

private val SmallChipPadding = 4.dp