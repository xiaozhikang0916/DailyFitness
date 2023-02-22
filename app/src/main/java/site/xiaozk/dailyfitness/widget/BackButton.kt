package site.xiaozk.dailyfitness.widget

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import site.xiaozk.dailyfitness.nav.LocalNavController

/**
 * @author: xiaozhikang
 * @mail: xiaozhikang0916@gmail.com
 * @create: 2023/3/3
 */

@Composable
fun BackButton() {
    val nav = LocalNavController.current
    IconButton(onClick = { nav?.popBackStack() }) {
        Icon(painter = rememberVectorPainter(image = Icons.Default.ArrowBack), contentDescription = "back")
    }
}