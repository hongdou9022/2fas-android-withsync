package com.twofasapp.cloudbackup.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CloudBackupManager {
    fun observeProviders(): Flow<List<CloudBackupProviderState>>
    fun observeOperationState(): StateFlow<CloudBackupOperationState>
    fun observeMaxBackups(): Flow<Int>
    fun setMaxBackups(count: Int)
    fun observeHistoryEnabled(): Flow<Boolean>
    fun setHistoryEnabled(enabled: Boolean)

    suspend fun backupNow(trigger: CloudBackupTrigger = CloudBackupTrigger.Manual): CloudBackupRunResult
    suspend fun listBackups(providerId: CloudBackupProviderId): CloudBackupResult<List<CloudBackupFile>>
    suspend fun deleteBackup(file: CloudBackupFile): CloudBackupResult<Unit>
    suspend fun restore(
        providerId: CloudBackupProviderId,
        remoteId: String,
        password: String? = null,
    ): CloudBackupResult<CloudBackupRestoreResult>
}
