package site.xiaozk.dailyfitness.page.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import site.xiaozk.dailyfitness.nav.LocalNavBackStack
import site.xiaozk.dailyfitness.settings.ui.ExportDataContent
import site.xiaozk.dailyfitness.settings.ui.ExportDataViewModel
import site.xiaozk.dailyfitness.settings.ui.R as SettingsUiR
import site.xiaozk.dailyfitness.widget.SubPageScaffold

/**
 * App-side shell of the data-export page: owns the app's [SubPageScaffold] (top bar
 * + back button) and hosts the `:settings-ui` ViewModel, while the form itself is
 * rendered by [ExportDataContent].
 *
 * Clicking export calls [ExportDataViewModel.export], which obtains the destination
 * directory from the app-injected
 * [site.xiaozk.dailyfitness.settings.ui.ExportDirectoryProvider].
 */
@Composable
fun ExportDataPage() {
    val viewModel: ExportDataViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val navBackStack = LocalNavBackStack.current
    SubPageScaffold(
        title = stringResource(SettingsUiR.string.export_title),
        onBack = { navBackStack.removeLastOrNull() },
    ) { scaffold ->
        ExportDataContent(
            state = state,
            contentPadding = scaffold.padding,
            onExport = viewModel::export,
        )
    }
}
