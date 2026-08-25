package com.twofasapp.feature.backup.ui.cloudbackup

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twofasapp.cloudbackup.api.CloudBackupProviderId
import com.twofasapp.cloudbackup.api.CloudBackupProviderState
import com.twofasapp.core.design.MdtIcons
import com.twofasapp.core.design.feature.settings.SettingsDivider
import com.twofasapp.core.design.feature.settings.SettingsHeader
import com.twofasapp.core.design.feature.settings.SettingsLink
import com.twofasapp.core.design.feature.settings.SettingsSwitch
import com.twofasapp.core.design.foundation.dialog.InputDialog
import com.twofasapp.core.design.foundation.progress.CircularProgressIndicator
import com.twofasapp.core.design.foundation.topbar.TopAppBar
import com.twofasapp.core.design.ktx.strings
import com.twofasapp.core.design.ktx.toastShort
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CloudBackupSettingsScreen(
    viewModel: CloudBackupSettingsViewModel = koinViewModel(),
    openProviderSettings: (CloudBackupProviderId) -> Unit,
    openProviderBackups: (CloudBackupProviderId) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CloudBackupSettingsContent(
        uiState = uiState,
        onSetMaxBackups = viewModel::setMaxBackups,
        onSetHistoryEnabled = viewModel::setHistoryEnabled,
        onBackupNow = viewModel::backupNow,
        onProviderSettings = openProviderSettings,
        onProviderBackups = openProviderBackups,
        onEventConsumed = viewModel::consumeEvent,
    )
}

@Composable
private fun CloudBackupSettingsContent(
    uiState: CloudBackupSettingsUiState,
    onSetMaxBackups: (Int) -> Unit = {},
    onSetHistoryEnabled: (Boolean) -> Unit = {},
    onBackupNow: () -> Unit = {},
    onProviderSettings: (CloudBackupProviderId) -> Unit = {},
    onProviderBackups: (CloudBackupProviderId) -> Unit = {},
    onEventConsumed: (CloudBackupSettingsUiEvent) -> Unit = {},
) {
    val context = LocalContext.current
    val strings = LocalContext.strings
    var showMaxBackupsDialog by remember { mutableStateOf(false) }

    uiState.events.firstOrNull()?.let { event ->
        LaunchedEffect(event) {
            context.toastShort(
                when (event) {
                    CloudBackupSettingsUiEvent.NoEnabledProviders -> strings.cloudBackupNoProvider
                    CloudBackupSettingsUiEvent.BackupCompleted -> strings.cloudBackupComplete
                    CloudBackupSettingsUiEvent.BackupPartiallyCompleted -> strings.cloudBackupPartial
                    CloudBackupSettingsUiEvent.BackupFailed -> strings.cloudBackupFailed
                },
            )
            onEventConsumed(event)
        }
    }

    Scaffold(
        topBar = { TopAppBar(titleText = strings.cloudBackupTitle) },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SettingsHeader(title = strings.cloudBackupProviders) }

            uiState.providers.forEach { provider ->
                item {
                    SettingsLink(
                        title = provider.name,
                        subtitle = provider.statusText(strings),
                        icon = if (provider.enabled) MdtIcons.Cloud else MdtIcons.CloudOff,
                        onClick = { onProviderSettings(provider.id) },
                    )
                }

                if (provider.configured) {
                    item {
                        SettingsLink(
                            title = "${strings.cloudBackupViewFiles}: ${provider.name}",
                            icon = MdtIcons.Download,
                            onClick = { onProviderBackups(provider.id) },
                        )
                    }
                }
            }

            item { SettingsDivider() }
            item { SettingsHeader(title = strings.cloudBackupOptions) }

            item {
                SettingsLink(
                    title = strings.cloudBackupMaxCount,
                    subtitle = "${strings.cloudBackupMaxCountDescription}\n${uiState.maxBackups}",
                    icon = MdtIcons.Settings,
                    onClick = { showMaxBackupsDialog = true },
                )
            }


            item {
                SettingsSwitch(
                    title = strings.cloudBackupHistory,
                    subtitle = strings.cloudBackupHistoryDescription,
                    icon = MdtIcons.Time,
                    checked = uiState.historyEnabled,
                    onCheckedChange = onSetHistoryEnabled,
                )
            }

            item {
                SettingsLink(
                    title = strings.cloudBackupNow,
                    subtitle = strings.cloudBackupNowDescription,
                    icon = MdtIcons.CloudUpload,
                    enabled = uiState.running.not(),
                    endContent = if (uiState.running) {
                        { CircularProgressIndicator() }
                    } else {
                        null
                    },
                    onClick = onBackupNow,
                )
            }
        }
    }

    if (showMaxBackupsDialog) {
        InputDialog(
            onDismissRequest = { showMaxBackupsDialog = false },
            title = strings.cloudBackupMaxCount,
            prefill = uiState.maxBackups.toString(),
            hint = "1-100",
            positive = strings.commonSave,
            negative = strings.commonCancel,
            positiveEnabled = { it.toIntOrNull() in 1..100 },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onPositiveClick = {
                it.toIntOrNull()?.let(onSetMaxBackups)
                showMaxBackupsDialog = false
            },
        )
    }
}

private fun CloudBackupProviderState.statusText(strings: com.twofasapp.locale.Strings): String {
    val status = when {
        configured.not() -> strings.cloudBackupStatusNotConfigured
        enabled -> strings.cloudBackupStatusEnabled
        else -> strings.cloudBackupStatusDisabled
    }
    return account?.takeIf { it.isNotBlank() }?.let { "$status\n$it" } ?: status
}
