package site.xiaozk.dailyfitness.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * Settings tab home: a simple list of entries into the dedicated settings pages.
 *
 * Purely presentational; the app shell owns the scaffold and navigation, so the
 * entry clicks are surfaced as callbacks.
 */
@Composable
fun SettingsHomeContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onOpenAiSettings: () -> Unit,
    onOpenExportData: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsEntry(
            title = stringResource(R.string.settings_ai_entry_title),
            summary = stringResource(R.string.settings_ai_entry_summary),
            onClick = onOpenAiSettings,
        )
        HorizontalDivider()
        SettingsEntry(
            title = stringResource(R.string.settings_export_entry_title),
            summary = stringResource(R.string.settings_export_entry_summary),
            onClick = onOpenExportData,
        )
        HorizontalDivider()
    }
}

@Composable
private fun SettingsEntry(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
