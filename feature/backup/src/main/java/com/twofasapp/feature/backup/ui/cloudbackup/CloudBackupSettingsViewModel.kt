package com.twofasapp.feature.backup.ui.cloudbackup

import androidx.lifecycle.ViewModel
import com.twofasapp.cloudbackup.api.CloudBackupManager
import com.twofasapp.cloudbackup.api.CloudBackupTrigger
import com.twofasapp.common.ktx.launchScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class CloudBackupSettingsViewModel(
    private val manager: CloudBackupManager,
) : ViewModel() {

    val uiState = MutableStateFlow(CloudBackupSettingsUiState())

    init {
        launchScoped {
            manager.observeProviders().collect { providers ->
                uiState.update { it.copy(providers = providers) }
            }
        }
        launchScoped {
            manager.observeMaxBackups().collect { count ->
                uiState.update { it.copy(maxBackups = count) }
            }
        }
    }

    fun setMaxBackups(count: Int) {
        manager.setMaxBackups(count)
    }

    fun backupNow() {
        if (uiState.value.providers.none { it.configured && it.enabled }) {
            publishEvent(CloudBackupSettingsUiEvent.NoEnabledProviders)
            return
        }

        launchScoped {
            uiState.update { it.copy(running = true) }
            val result = manager.backupNow(CloudBackupTrigger.Manual)
            uiState.update { it.copy(running = false) }
            publishEvent(
                when {
                    result.failedProviders.isEmpty() -> CloudBackupSettingsUiEvent.BackupCompleted
                    result.successfulProviders.isEmpty() -> CloudBackupSettingsUiEvent.BackupFailed
                    else -> CloudBackupSettingsUiEvent.BackupPartiallyCompleted
                },
            )
        }
    }

    fun consumeEvent(event: CloudBackupSettingsUiEvent) {
        uiState.update { it.copy(events = it.events.minus(event)) }
    }

    private fun publishEvent(event: CloudBackupSettingsUiEvent) {
        uiState.update { it.copy(events = it.events.plus(event)) }
    }
}
