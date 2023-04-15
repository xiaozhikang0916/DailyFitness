package site.xiaozk.dailyfitness.widget

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import site.xiaozk.dailyfitness.R

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/3
 */

@Composable
fun BackButton(icon: ImageVector = Icons.Default.KeyboardArrowLeft, onBackClick: () -> Unit = {}) {
    IconButton(onClick = onBackClick) {
        Icon(painter = rememberVectorPainter(image = icon), contentDescription = stringResource(R.string.top_action_back))
    }
}