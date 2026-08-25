package com.twofasapp.feature.backup.ui.backupsettings

import androidx.lifecycle.ViewModel
import com.twofasapp.common.ktx.launchScoped
import com.twofasapp.data.services.BackupRepository
import com.twofasapp.data.services.domain.CloudSyncTrigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class BackupSettingsViewModel(
    private val backupRepository: BackupRepository,
) : ViewModel() {

    val uiState: MutableStateFlow<BackupSettingsUiState> = MutableStateFlow(BackupSettingsUiState())

    init {
        launchScoped {
            backupRepository.observeCloudBackupStatus().collect { cloudBackupStatus ->
                uiState.update {
                    it.copy(
                        syncActive = cloudBackupStatus.active,
                        account = cloudBackupStatus.account.orEmpty(),
                        encrypted = cloudBackupStatus.encrypted,
                        lastSyncMillis = cloudBackupStatus.lastSyncMillis,
                    )
                }
            }
        }

        launchScoped {
            backupRepository.observeCloudSyncStatus().collect { syncStatus ->
                uiState.update {
                    it.copy(syncStatus = syncStatus)
                }
            }
        }

        launchScoped {
            backupRepository.observeBackupPasswordSet().collect { passwordSet ->
                uiState.update { it.copy(passwordSet = passwordSet) }
            }
        }
    }

    fun setPassword(password: String) {
        launchScoped {
            backupRepository.setBackupPassword(password)
            if (uiState.value.syncActive) {
                backupRepository.dispatchCloudSync(CloudSyncTrigger.SetPassword, password)
            }
        }
    }

    fun removePassword(password: String) {
        launchScoped {
            val isCorrect = if (uiState.value.syncActive && uiState.value.encrypted) {
                backupRepository.checkCloudBackupPassword(password)
            } else {
                backupRepository.checkBackupPassword(password)
            }

            if (isCorrect.not()) {
                publishEvent(BackupSettingsUiEvent.ShowRemovePasswordDialogError)
                return@launchScoped
            }

            if (uiState.value.syncActive) {
                backupRepository.dispatchCloudSync(CloudSyncTrigger.RemovePassword, password)
            } else {
                backupRepository.setBackupPassword(null)
            }
        }
    }

    fun deleteBackup(password: String?) {
        launchScoped {
            if (uiState.value.encrypted) {
                val isCorrect = backupRepository.checkCloudBackupPassword(password)

                if (isCorrect) {
                    backupRepository.dispatchWipeData()
                    publishEvent(BackupSettingsUiEvent.Finish)
                } else {
                    publishEvent(BackupSettingsUiEvent.ShowWipePasswordDialogError)
                }
            } else {
                backupRepository.dispatchWipeData()
                publishEvent(BackupSettingsUiEvent.Finish)
            }
        }
    }

    fun consumeEvent(event: BackupSettingsUiEvent) {
        uiState.update { it.copy(events = it.events.minus(event)) }
    }

    private fun publishEvent(event: BackupSettingsUiEvent) {
        uiState.update { it.copy(events = it.events.plus(event)) }
    }
}
