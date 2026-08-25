package com.twofasapp.cloudbackup.core

import com.twofasapp.cloudbackup.api.CloudBackupError
import com.twofasapp.cloudbackup.api.CloudBackupFile
import com.twofasapp.cloudbackup.api.CloudBackupManager
import com.twofasapp.cloudbackup.api.CloudBackupOperationState
import com.twofasapp.cloudbackup.api.CloudBackupProvider
import com.twofasapp.cloudbackup.api.CloudBackupProviderId
import com.twofasapp.cloudbackup.api.CloudBackupProviderState
import com.twofasapp.cloudbackup.api.CloudBackupRestoreResult
import com.twofasapp.cloudbackup.api.CloudBackupResult
import com.twofasapp.cloudbackup.api.CloudBackupRunResult
import com.twofasapp.cloudbackup.api.CloudBackupTrigger
import com.twofasapp.data.services.BackupRepository
import com.twofasapp.data.services.exceptions.DecryptWrongPassword
import com.twofasapp.prefs.model.isSet
import com.twofasapp.prefs.usecase.RemoteBackupKeyPreference
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class CloudBackupManagerImpl(
    private val backupRepository: BackupRepository,
    private val remoteBackupKeyPreference: RemoteBackupKeyPreference,
    private val settingsStore: CloudBackupSettingsStore,
    private val providers: List<CloudBackupProvider>,
) : CloudBackupManager {

    companion object {
        const val FilePrefix = "2fas-backup-"
        const val FileExtension = ".2fas"
        const val HistoryFileName = "2fas-backup-history.json"
        private const val MaxFileSize = 100 * 1024 * 1024
        private val FileNameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
        private val HistoryTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        private val BackupFileRegex = Regex("^2fas-backup-\\d{8}-\\d{6}-\\d{3}\\.2fas$")
    }

    private val operationState = MutableStateFlow<CloudBackupOperationState>(CloudBackupOperationState.Idle)
    private val backupMutex = Mutex()

    override fun observeProviders(): Flow<List<CloudBackupProviderState>> {
        if (providers.isEmpty()) return flowOf(emptyList())
        return combine(providers.map { it.observeState() }) { states -> states.toList() }
    }

    override fun observeOperationState(): StateFlow<CloudBackupOperationState> = operationState

    override fun observeMaxBackups(): Flow<Int> = settingsStore.observeMaxBackups()

    override fun setMaxBackups(count: Int) = settingsStore.setMaxBackups(count)

    override fun observeHistoryEnabled(): Flow<Boolean> = settingsStore.observeHistoryEnabled()

    override fun setHistoryEnabled(enabled: Boolean) = settingsStore.setHistoryEnabled(enabled)

    override suspend fun backupNow(trigger: CloudBackupTrigger): CloudBackupRunResult = backupMutex.withLock {
        val enabledProviders = providers.filter { it.state().configured && it.state().enabled }
        if (enabledProviders.isEmpty()) {
            return@withLock CloudBackupRunResult(emptyList(), emptyMap())
        }

        operationState.value = CloudBackupOperationState.Running(trigger)
        val historyEnabled = settingsStore.isHistoryEnabled()
        val contentAndSnapshot = try {
            val content = backupRepository.createBackupContentSerializedWithBackupKey()
            val snapshot = if (historyEnabled) {
                CloudBackupSnapshot.from(backupRepository.createBackupContent().backupContent)
            } else {
                null
            }
            content to snapshot
        } catch (e: Exception) {
            return@withLock completeWithFailures(enabledProviders, CloudBackupError.Unknown)
        }
        val localTime = LocalDateTime.now()
        val fileName = buildFileName(localTime)
        val createdAt = localTime.format(HistoryTimeFormatter)

        val results = coroutineScope {
            enabledProviders.map { provider ->
                async {
                    provider.id to try {
                        uploadAndRotate(
                            provider = provider,
                            fileName = fileName,
                            content = contentAndSnapshot.first,
                            createdAt = createdAt,
                            trigger = trigger,
                            snapshot = contentAndSnapshot.second,
                        )
                    } catch (e: Exception) {
                        CloudBackupResult.Failure(CloudBackupError.Provider(e.message))
                    }
                }
            }.awaitAll()
        }

        val runResult = CloudBackupRunResult(
            successfulProviders = results.mapNotNull { (id, result) -> id.takeIf { result is CloudBackupResult.Success } },
            failedProviders = results.mapNotNull { (id, result) ->
                (result as? CloudBackupResult.Failure)?.let { id to it.error }
            }.toMap(),
        )
        operationState.value = CloudBackupOperationState.Completed(runResult)
        runResult
    }

    override suspend fun listBackups(providerId: CloudBackupProviderId): CloudBackupResult<List<CloudBackupFile>> {
        val provider = findProvider(providerId) ?: return CloudBackupResult.Failure(CloudBackupError.NotConfigured)
        return when (val result = provider.listBackups()) {
            is CloudBackupResult.Failure -> result
            is CloudBackupResult.Success -> CloudBackupResult.Success(
                result.value
                    .filter { isBackupFile(it.name) }
                    .sortedByDescending { it.name },
            )
        }
    }

    override suspend fun deleteBackup(file: CloudBackupFile): CloudBackupResult<Unit> = backupMutex.withLock {
        if (isBackupFile(file.name).not()) {
            return@withLock CloudBackupResult.Failure(CloudBackupError.InvalidBackup)
        }
        val provider = findProvider(file.providerId)
            ?: return@withLock CloudBackupResult.Failure(CloudBackupError.NotConfigured)

        when (val delete = provider.delete(file.remoteId)) {
            is CloudBackupResult.Failure -> delete
            is CloudBackupResult.Success -> removeHistoryEntries(provider, setOf(file.name))
        }
    }

    override suspend fun restore(
        providerId: CloudBackupProviderId,
        remoteId: String,
        password: String?,
    ): CloudBackupResult<CloudBackupRestoreResult> {
        val provider = findProvider(providerId) ?: return CloudBackupResult.Failure(CloudBackupError.NotConfigured)
        return try {
            when (val download = provider.download(remoteId)) {
                is CloudBackupResult.Failure -> download
                is CloudBackupResult.Success -> {
                    if (download.value.toByteArray(Charsets.UTF_8).size > MaxFileSize) {
                        return CloudBackupResult.Failure(CloudBackupError.InvalidBackup)
                    }

                    var backupContent = backupRepository.readBackupContentSerialized(download.value)
                    if (backupContent.isEncrypted) {
                        val key = remoteBackupKeyPreference.get()
                        backupContent = try {
                            when {
                                password.isNullOrEmpty().not() -> backupRepository.decryptBackupContent(backupContent, password = password)
                                key.isSet() -> backupRepository.decryptBackupContent(backupContent, keyEncoded = key.keyEncoded)
                                else -> return CloudBackupResult.Failure(CloudBackupError.PasswordRequired)
                            }
                        } catch (e: DecryptWrongPassword) {
                            return CloudBackupResult.Failure(
                                if (password.isNullOrEmpty()) CloudBackupError.PasswordRequired else CloudBackupError.WrongPassword,
                            )
                        }
                    }

                    backupRepository.import(backupContent, triggerCloudBackup = false)
                    CloudBackupResult.Success(
                        CloudBackupRestoreResult(
                            servicesCount = backupContent.services.size,
                            groupsCount = backupContent.groups.size,
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            CloudBackupResult.Failure(CloudBackupError.InvalidBackup)
        }
    }

    private suspend fun uploadAndRotate(
        provider: CloudBackupProvider,
        fileName: String,
        content: String,
        createdAt: String,
        trigger: CloudBackupTrigger,
        snapshot: CloudBackupSnapshot?,
    ): CloudBackupResult<Unit> {
        val upload = provider.upload(fileName, content)
        if (upload is CloudBackupResult.Failure) return upload

        val listed = provider.listBackups()
        if (listed is CloudBackupResult.Failure) return listed

        val oldFiles = (listed as CloudBackupResult.Success)
            .value
            .filter { isBackupFile(it.name) }
            .sortedByDescending { it.name }
            .drop(settingsStore.getMaxBackups())

        oldFiles.forEach { file ->
            val delete = provider.delete(file.remoteId)
            if (delete is CloudBackupResult.Failure) return delete
        }

        val deletedNames = oldFiles.mapTo(mutableSetOf()) { it.name }
        val retainedBackups = listed.value
            .asSequence()
            .filter { isBackupFile(it.name) && it.name !in deletedNames }
            .map { it.name }
            .toSet()

        if (snapshot != null) {
            val history = when (val result = readHistory(provider, listed.value)) {
                is CloudBackupResult.Failure -> return result
                is CloudBackupResult.Success -> result.value ?: CloudBackupHistoryDocument()
            }
            val updated = history.append(
                fileName = fileName,
                createdAt = createdAt,
                trigger = trigger,
                snapshot = snapshot,
                retainedBackups = retainedBackups,
            )
            return provider.upload(HistoryFileName, CloudBackupHistoryCodec.encode(updated))
        }

        if (deletedNames.isNotEmpty()) {
            return removeHistoryEntries(provider, deletedNames, listed.value)
        }

        return CloudBackupResult.Success(Unit)
    }

    private suspend fun removeHistoryEntries(
        provider: CloudBackupProvider,
        fileNames: Set<String>,
        listedFiles: List<CloudBackupFile>? = null,
    ): CloudBackupResult<Unit> {
        val files = listedFiles ?: when (val listed = provider.listBackups()) {
            is CloudBackupResult.Failure -> return listed
            is CloudBackupResult.Success -> listed.value
        }
        return when (val history = readHistory(provider, files)) {
            is CloudBackupResult.Failure -> history
            is CloudBackupResult.Success -> {
                val document = history.value ?: return CloudBackupResult.Success(Unit)
                provider.upload(HistoryFileName, CloudBackupHistoryCodec.encode(document.remove(fileNames)))
            }
        }
    }

    private suspend fun readHistory(
        provider: CloudBackupProvider,
        listedFiles: List<CloudBackupFile>,
    ): CloudBackupResult<CloudBackupHistoryDocument?> {
        val historyFile = listedFiles.firstOrNull { it.name == HistoryFileName }
            ?: return CloudBackupResult.Success(null)
        return when (val download = provider.download(historyFile.remoteId)) {
            is CloudBackupResult.Failure -> download
            is CloudBackupResult.Success -> CloudBackupResult.Success(
                runCatching { CloudBackupHistoryCodec.decode(download.value) }.getOrNull(),
            )
        }
    }

    private fun findProvider(id: CloudBackupProviderId) = providers.firstOrNull { it.id == id }

    private fun buildFileName(localTime: LocalDateTime): String =
        "$FilePrefix${localTime.format(FileNameFormatter)}$FileExtension"

    private fun completeWithFailures(
        providers: List<CloudBackupProvider>,
        error: CloudBackupError,
    ): CloudBackupRunResult {
        val result = CloudBackupRunResult(
            successfulProviders = emptyList(),
            failedProviders = providers.associate { it.id to error },
        )
        operationState.value = CloudBackupOperationState.Completed(result)
        return result
    }

    private fun isBackupFile(name: String): Boolean = BackupFileRegex.matches(name)
}
