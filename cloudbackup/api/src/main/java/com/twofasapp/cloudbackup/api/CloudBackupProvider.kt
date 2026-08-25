package com.twofasapp.cloudbackup.api

import kotlinx.coroutines.flow.Flow

interface CloudBackupProvider {
    val id: CloudBackupProviderId

    fun state(): CloudBackupProviderState
    fun observeState(): Flow<CloudBackupProviderState>

    suspend fun upload(fileName: String, content: String): CloudBackupResult<Unit>
    suspend fun listBackups(): CloudBackupResult<List<CloudBackupFile>>
    suspend fun download(remoteId: String): CloudBackupResult<String>
    suspend fun delete(remoteId: String): CloudBackupResult<Unit>
}
