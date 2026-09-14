package site.xiaozk.dailyfitness.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import site.xiaozk.dailyfitness.R
import site.xiaozk.dailyfitness.repository.model.BodyField

/**
 * @author: xiaozhikang
 * @create: 2023/4/15
 */

/**
 * Display mapping for [BodyField], kept in the UI layer so the repository model stays free
 * of Android resources.
 */
@get:StringRes
val BodyField.labelRes: Int
    get() = when (this) {
        BodyField.Weight -> R.string.label_body_weight
        BodyField.Bust -> R.string.label_bust_size
        BodyField.Waist -> R.string.label_waist_size
        BodyField.Hip -> R.string.label_hip_size
        BodyField.BodyFat -> R.string.label_body_fat
    }

@get:StringRes
val BodyField.trailingRes: Int
    get() = when (this) {
        BodyField.Weight -> R.string.label_weight_unit_kg
        BodyField.Bust, BodyField.Waist, BodyField.Hip -> R.string.label_length_unit_cm
        BodyField.BodyFat -> R.string.label_count_unit_percentage
    }

val BodyField.label: String
    @Composable get() = stringResource(id = labelRes)

val BodyField.trailing: String
    @Composable get() = stringResource(id = trailingRes)
