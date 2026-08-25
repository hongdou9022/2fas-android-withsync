package com.twofasapp.feature.backup.ui.cloudbackup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.twofasapp.android.navigation.NavArg
import com.twofasapp.cloudbackup.api.CloudBackupError
import com.twofasapp.cloudbackup.api.CloudBackupFile
import com.twofasapp.cloudbackup.api.CloudBackupManager
import com.twofasapp.cloudbackup.api.CloudBackupProviderId
import com.twofasapp.cloudbackup.api.CloudBackupResult
import com.twofasapp.common.ktx.launchScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class CloudBackupFilesViewModel(
    savedStateHandle: SavedStateHandle,
    private val manager: CloudBackupManager,
) : ViewModel() {

    private val providerId = CloudBackupProviderId(
        savedStateHandle.get<String>(NavArg.CloudBackupProviderId.name).orEmpty(),
    )

    val uiState = MutableStateFlow(CloudBackupFilesUiState())

    init {
        launchScoped {
            manager.observeProviders().collect { providers ->
                val provider = providers.firstOrNull { it.id == providerId }
                uiState.update { it.copy(providerName = provider?.name.orEmpty()) }
            }
        }
        refresh()
    }

    fun refresh() {
        launchScoped {
            uiState.update { it.copy(loading = true) }
            when (val result = manager.listBackups(providerId)) {
                is CloudBackupResult.Success -> uiState.update {
                    it.copy(files = result.value, loading = false)
                }

                is CloudBackupResult.Failure -> {
                    uiState.update { it.copy(loading = false) }
                    publishEvent(CloudBackupFilesUiEvent.ListFailed)
                }
            }
        }
    }

    fun restore(file: CloudBackupFile, password: String? = null) {
        if (uiState.value.restoringRemoteId != null || uiState.value.deletingRemoteId != null) return
        launchScoped {
            uiState.update { it.copy(restoringRemoteId = file.remoteId) }
            when (val result = manager.restore(providerId, file.remoteId, password)) {
                is CloudBackupResult.Success -> publishEvent(CloudBackupFilesUiEvent.RestoreSuccess)
                is CloudBackupResult.Failure -> when (result.error) {
                    CloudBackupError.PasswordRequired -> publishEvent(
                        CloudBackupFilesUiEvent.RequestPassword(file, wrongPassword = false),
                    )

                    CloudBackupError.WrongPassword -> publishEvent(
                        CloudBackupFilesUiEvent.RequestPassword(file, wrongPassword = true),
                    )

                    else -> publishEvent(CloudBackupFilesUiEvent.OperationFailed)
                }
            }
            uiState.update { it.copy(restoringRemoteId = null) }
        }
    }

    fun delete(file: CloudBackupFile) {
        if (uiState.value.restoringRemoteId != null || uiState.value.deletingRemoteId != null) return
        launchScoped {
            uiState.update { it.copy(deletingRemoteId = file.remoteId) }
            when (manager.deleteBackup(file)) {
                is CloudBackupResult.Success -> {
                    uiState.update { state ->
                        state.copy(files = state.files.filterNot { it.remoteId == file.remoteId })
                    }
                    publishEvent(CloudBackupFilesUiEvent.DeleteSuccess)
                }

                is CloudBackupResult.Failure -> publishEvent(CloudBackupFilesUiEvent.DeleteFailed)
            }
            uiState.update { it.copy(deletingRemoteId = null) }
        }
    }

    fun consumeEvent(event: CloudBackupFilesUiEvent) {
        uiState.update { it.copy(events = it.events.minus(event)) }
    }

    private fun publishEvent(event: CloudBackupFilesUiEvent) {
        uiState.update { it.copy(events = it.events.plus(event)) }
    }
}
