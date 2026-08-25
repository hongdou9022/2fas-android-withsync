package com.twofasapp.cloudbackup.webdav

import com.twofasapp.cloudbackup.api.CloudBackupFile
import com.twofasapp.cloudbackup.api.CloudBackupProvider
import com.twofasapp.cloudbackup.api.CloudBackupProviderId
import com.twofasapp.cloudbackup.api.CloudBackupProviderState
import com.twofasapp.cloudbackup.api.CloudBackupResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class WebDavCloudBackupProvider(
    private val configStore: WebDavConfigStore,
    private val client: WebDavClient,
) : CloudBackupProvider {

    override val id = CloudBackupProviderId("webdav")

    override fun state(): CloudBackupProviderState = configStore.get().asProviderState()

    override fun observeState(): Flow<CloudBackupProviderState> = configStore.observe().map { it.asProviderState() }

    override suspend fun upload(fileName: String, content: String): CloudBackupResult<Unit> =
        client.upload(configStore.get(), fileName, content)

    override suspend fun listBackups(): CloudBackupResult<List<CloudBackupFile>> =
        client.list(configStore.get(), id)

    override suspend fun download(remoteId: String): CloudBackupResult<String> =
        client.download(configStore.get(), remoteId)

    override suspend fun delete(remoteId: String): CloudBackupResult<Unit> =
        client.delete(configStore.get(), remoteId)

    private fun WebDavConfig.asProviderState() = CloudBackupProviderState(
        id = id,
        name = "WebDAV",
        configured = configured,
        enabled = enabled,
        account = username.takeIf { it.isNotBlank() },
    )
}
