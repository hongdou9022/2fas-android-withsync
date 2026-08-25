package com.twofasapp.cloudbackup.webdav

import com.twofasapp.cloudbackup.api.CloudBackupResult
import kotlinx.coroutines.flow.Flow

interface WebDavRepository {
    fun observeConfig(): Flow<WebDavConfig>
    fun save(config: WebDavConfig)
    suspend fun testConnection(config: WebDavConfig): CloudBackupResult<Unit>
}
