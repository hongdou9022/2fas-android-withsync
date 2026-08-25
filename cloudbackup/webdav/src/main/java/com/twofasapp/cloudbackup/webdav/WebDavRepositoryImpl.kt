package com.twofasapp.cloudbackup.webdav

import com.twofasapp.cloudbackup.api.CloudBackupResult
import kotlinx.coroutines.flow.Flow

internal class WebDavRepositoryImpl(
    private val configStore: WebDavConfigStore,
    private val client: WebDavClient,
) : WebDavRepository {
    override fun observeConfig(): Flow<WebDavConfig> = configStore.observe()

    override fun save(config: WebDavConfig) = configStore.save(config)

    override suspend fun testConnection(config: WebDavConfig): CloudBackupResult<Unit> = client.testConnection(config)
}
