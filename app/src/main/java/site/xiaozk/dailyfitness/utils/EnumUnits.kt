package site.xiaozk.dailyfitness.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import site.xiaozk.dailyfitness.repository.model.BodyField

/**
 * @author: xiaozhikang
 * @create: 2023/4/15
 */

val BodyField.label: String
    @Composable get() = stringResource(id = this.labelRes)
val BodyField.trailing: String
    @Composable get() = stringResource(id = this.trailingRes)