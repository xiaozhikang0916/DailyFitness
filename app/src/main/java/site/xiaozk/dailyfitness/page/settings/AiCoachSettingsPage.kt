package site.xiaozk.dailyfitness.page.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.LocalNavBackStack
import site.xiaozk.dailyfitness.settings.R as SettingsR
import site.xiaozk.dailyfitness.settings.ai.AiCoachSettingsContent
import site.xiaozk.dailyfitness.settings.ai.AiCoachSettingsViewModel
import site.xiaozk.dailyfitness.widget.SubPageScaffold

/**
 * App-side shell of the AI Coach settings page: owns the app's [SubPageScaffold]
 * (top bar + back button) and hosts the `:settings` ViewModel, while the form
 * itself is rendered by [AiCoachSettingsContent].
 */
@Composable
fun AiCoachSettingsPage() {
    val viewModel: AiCoachSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val navBackStack = LocalNavBackStack.current
    SubPageScaffold(
        title = stringResource(SettingsR.string.ai_settings_title),
        onBack = { navBackStack.removeLastOrNull() },
    ) { scaffold ->
        AiCoachSettingsContent(
            state = state,
            contentPadding = scaffold.padding,
            onApiKeyChange = viewModel::onApiKeyChange,
            onModelChange = viewModel::onModelChange,
            onBaseUrlChange = viewModel::onBaseUrlChange,
            onSave = viewModel::save,
        )
    }
}
