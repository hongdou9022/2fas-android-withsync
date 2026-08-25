package com.twofasapp.feature.backup.ui.cloudbackup

import com.twofasapp.cloudbackup.api.CloudBackupFile

internal data class CloudBackupFilesUiState(
    val providerName: String = "",
    val files: List<CloudBackupFile> = emptyList(),
    val loading: Boolean = true,
    val restoringRemoteId: String? = null,
    val deletingRemoteId: String? = null,
    val events: List<CloudBackupFilesUiEvent> = emptyList(),
)

internal sealed interface CloudBackupFilesUiEvent {
    data class RequestPassword(
        val file: CloudBackupFile,
        val wrongPassword: Boolean,
    ) : CloudBackupFilesUiEvent

    data object RestoreSuccess : CloudBackupFilesUiEvent
    data object DeleteSuccess : CloudBackupFilesUiEvent
    data object DeleteFailed : CloudBackupFilesUiEvent
    data object ListFailed : CloudBackupFilesUiEvent
    data object OperationFailed : CloudBackupFilesUiEvent
}
