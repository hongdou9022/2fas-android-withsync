package com.twofasapp.feature.backup.ui.cloudbackup

import com.twofasapp.cloudbackup.api.CloudBackupProviderState

internal data class CloudBackupSettingsUiState(
    val providers: List<CloudBackupProviderState> = emptyList(),
    val maxBackups: Int = 10,
    val historyEnabled: Boolean = true,
    val running: Boolean = false,
    val events: List<CloudBackupSettingsUiEvent> = emptyList(),
)

internal sealed interface CloudBackupSettingsUiEvent {
    data object NoEnabledProviders : CloudBackupSettingsUiEvent
    data object BackupCompleted : CloudBackupSettingsUiEvent
    data object BackupPartiallyCompleted : CloudBackupSettingsUiEvent
    data object BackupFailed : CloudBackupSettingsUiEvent
}
