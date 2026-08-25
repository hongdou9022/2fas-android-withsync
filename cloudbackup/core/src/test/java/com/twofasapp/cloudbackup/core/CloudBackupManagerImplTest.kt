package com.twofasapp.cloudbackup.core

import com.twofasapp.cloudbackup.api.CloudBackupError
import com.twofasapp.cloudbackup.api.CloudBackupFile
import com.twofasapp.cloudbackup.api.CloudBackupProvider
import com.twofasapp.cloudbackup.api.CloudBackupProviderId
import com.twofasapp.cloudbackup.api.CloudBackupProviderState
import com.twofasapp.cloudbackup.api.CloudBackupResult
import com.twofasapp.cloudbackup.api.CloudBackupTrigger
import com.twofasapp.data.services.BackupRepository
import com.twofasapp.data.services.domain.BackupContent
import com.twofasapp.data.services.domain.BackupContentCreateResult
import com.twofasapp.data.services.domain.BackupGroup
import com.twofasapp.prefs.usecase.RemoteBackupKeyPreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBackupManagerImplTest {

    private val backupRepository = mockk<BackupRepository>()
    private val keyPreference = mockk<RemoteBackupKeyPreference>(relaxed = true)
    private val settingsStore = mockk<CloudBackupSettingsStore> {
        every { getMaxBackups() } returns 10
        every { isHistoryEnabled() } returns false
    }

    @Test
    fun `backup uploads to every enabled provider using local timestamp name`() = runTest {
        coEvery { backupRepository.createBackupContentSerializedWithBackupKey() } returns "backup-content"
        every { settingsStore.getMaxBackups() } returns 10
        val first = FakeProvider("first")
        val second = FakeProvider("second")
        val manager = manager(first, second)

        val result = manager.backupNow()

        assertEquals(listOf(first.id, second.id), result.successfulProviders)
        assertTrue(result.failedProviders.isEmpty())
        assertEquals("backup-content", first.uploaded.single().second)
        assertTrue(
            Regex("^2fas-backup-\\d{8}-\\d{6}-\\d{3}\\.2fas$")
                .matches(first.uploaded.single().first),
        )
        assertEquals(first.uploaded.single().first, second.uploaded.single().first)
    }

    @Test
    fun `rotation deletes oldest timestamped filename and keeps unrelated files`() = runTest {
        coEvery { backupRepository.createBackupContentSerializedWithBackupKey() } returns "content"
        every { settingsStore.getMaxBackups() } returns 2
        val provider = FakeProvider(
            idValue = "webdav",
            initialFiles = listOf(
                file("notes.txt"),
                file("2fas-backup-not-a-snapshot.2fas"),
                file("2fas-backup-20260101-100000-000.2fas"),
                file("2fas-backup-20260201-100000-000.2fas"),
            ),
        )

        manager(provider).backupNow()

        assertEquals(listOf("2fas-backup-20260101-100000-000.2fas"), provider.deleted)
        assertTrue(provider.files.any { it.name == "notes.txt" })
        assertTrue(provider.files.any { it.name == "2fas-backup-not-a-snapshot.2fas" })
    }

    @Test
    fun `disabled providers do not create backup content`() = runTest {
        val provider = FakeProvider("webdav", enabled = false)

        val result = manager(provider).backupNow()

        assertTrue(result.successfulProviders.isEmpty())
        assertTrue(result.failedProviders.isEmpty())
        coVerify(exactly = 0) { backupRepository.createBackupContentSerializedWithBackupKey() }
    }

    @Test
    fun `content creation failure is reported for every enabled provider`() = runTest {
        coEvery { backupRepository.createBackupContentSerializedWithBackupKey() } throws IllegalStateException("failed")
        val first = FakeProvider("first")
        val second = FakeProvider("second")

        val result = manager(first, second).backupNow()

        assertEquals(setOf(first.id, second.id), result.failedProviders.keys)
        assertTrue(result.failedProviders.values.all { it == CloudBackupError.Unknown })
    }

    @Test
    fun `backup list filters unrelated files and sorts by filename timestamp`() = runTest {
        val provider = FakeProvider(
            idValue = "webdav",
            initialFiles = listOf(
                file("notes.txt"),
                file("2fas-backup-not-a-snapshot.2fas"),
                file("2fas-backup-20260101-100000-000.2fas"),
                file("2fas-backup-20260301-100000-000.2fas"),
                file("2fas-backup-20260201-100000-000.2fas"),
            ),
        )

        val result = manager(provider).listBackups(provider.id) as CloudBackupResult.Success

        assertEquals(
            listOf(
                "2fas-backup-20260301-100000-000.2fas",
                "2fas-backup-20260201-100000-000.2fas",
                "2fas-backup-20260101-100000-000.2fas",
            ),
            result.value.map { it.name },
        )
    }

    @Test
    fun `enabled history creates local-time entry and records deltas`() = runTest {
        every { settingsStore.isHistoryEnabled() } returns true
        coEvery { backupRepository.createBackupContentSerializedWithBackupKey() } returns "backup-content"
        coEvery { backupRepository.createBackupContent() } returns BackupContentCreateResult(
            BackupContent(groups = listOf(group("group-1", "Personal"))),
        )
        val provider = FakeProvider("webdav")

        val result = manager(provider).backupNow(CloudBackupTrigger.Manual)

        assertEquals(listOf(provider.id), result.successfulProviders)
        val history = CloudBackupHistoryCodec.decode(provider.contents.getValue(CloudBackupManagerImpl.HistoryFileName))
        val entry = history.entries.single()
        assertTrue(Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}$").matches(entry.createdAt))
        assertEquals("Manual", entry.trigger)
        assertEquals(listOf("Personal"), entry.groupsAdded)
        assertEquals(entry.fileName, history.latestBackup)
    }

    @Test
    fun `history document records added removed and changed items`() {
        val firstFile = "2fas-backup-20260101-100000-000.2fas"
        val secondFile = "2fas-backup-20260102-100000-000.2fas"
        val first = CloudBackupHistoryDocument().append(
            fileName = firstFile,
            createdAt = "2026-01-01 10:00:00.000",
            trigger = CloudBackupTrigger.ServicesChanged,
            snapshot = CloudBackupSnapshot(
                services = listOf(CloudBackupSnapshotItem("a", "one", "Alpha")),
                groups = listOf(CloudBackupSnapshotItem("g", "one", "Personal")),
            ),
            retainedBackups = setOf(firstFile),
        )

        val second = first.append(
            fileName = secondFile,
            createdAt = "2026-01-02 10:00:00.000",
            trigger = CloudBackupTrigger.Manual,
            snapshot = CloudBackupSnapshot(
                services = listOf(
                    CloudBackupSnapshotItem("a", "two", "Alpha renamed"),
                    CloudBackupSnapshotItem("b", "one", "Beta"),
                ),
            ),
            retainedBackups = setOf(firstFile, secondFile),
        )

        val entry = second.entries.last()
        assertEquals(listOf("Beta"), entry.servicesAdded)
        assertEquals(emptyList<String>(), entry.servicesRemoved)
        assertEquals(listOf("Alpha renamed"), entry.servicesChanged)
        assertEquals(listOf("Personal"), entry.groupsRemoved)
    }

    @Test
    fun `rotation removes matching history entry`() = runTest {
        every { settingsStore.getMaxBackups() } returns 2
        every { settingsStore.isHistoryEnabled() } returns true
        coEvery { backupRepository.createBackupContentSerializedWithBackupKey() } returns "backup-content"
        coEvery { backupRepository.createBackupContent() } returns BackupContentCreateResult(BackupContent.Empty)
        val firstFile = "2fas-backup-20260101-100000-000.2fas"
        val secondFile = "2fas-backup-20260201-100000-000.2fas"
        val history = CloudBackupHistoryDocument()
            .append(
                firstFile,
                "2026-01-01 10:00:00.000",
                CloudBackupTrigger.ServicesChanged,
                CloudBackupSnapshot(),
                setOf(firstFile),
            )
            .append(
                secondFile,
                "2026-02-01 10:00:00.000",
                CloudBackupTrigger.ServicesChanged,
                CloudBackupSnapshot(),
                setOf(firstFile, secondFile),
            )
        val provider = FakeProvider(
            idValue = "webdav",
            initialFiles = listOf(file(firstFile), file(secondFile), file(CloudBackupManagerImpl.HistoryFileName)),
            initialContents = mapOf(
                CloudBackupManagerImpl.HistoryFileName to CloudBackupHistoryCodec.encode(history),
            ),
        )

        manager(provider).backupNow()

        assertTrue(firstFile in provider.deleted)
        val updated = CloudBackupHistoryCodec.decode(provider.contents.getValue(CloudBackupManagerImpl.HistoryFileName))
        assertTrue(updated.entries.none { it.fileName == firstFile })
        assertTrue(updated.entries.any { it.fileName == secondFile })
    }

    @Test
    fun `manual delete removes backup and matching history entry even when history is disabled`() = runTest {
        val backupFile = "2fas-backup-20260101-100000-000.2fas"
        val history = CloudBackupHistoryDocument().append(
            backupFile,
            "2026-01-01 10:00:00.000",
            CloudBackupTrigger.Manual,
            CloudBackupSnapshot(),
            setOf(backupFile),
        )
        val provider = FakeProvider(
            idValue = "webdav",
            initialFiles = listOf(file(backupFile), file(CloudBackupManagerImpl.HistoryFileName)),
            initialContents = mapOf(
                backupFile to "backup-content",
                CloudBackupManagerImpl.HistoryFileName to CloudBackupHistoryCodec.encode(history),
            ),
        )

        val result = manager(provider).deleteBackup(file(backupFile))

        assertTrue(result is CloudBackupResult.Success)
        assertTrue(backupFile in provider.deleted)
        val updated = CloudBackupHistoryCodec.decode(provider.contents.getValue(CloudBackupManagerImpl.HistoryFileName))
        assertTrue(updated.entries.isEmpty())
        assertEquals(null, updated.latestBackup)
    }

    @Test
    fun `restore imports without triggering another cloud backup`() = runTest {
        val backupFile = "2fas-backup-20260101-100000-000.2fas"
        val provider = FakeProvider(
            idValue = "webdav",
            initialFiles = listOf(file(backupFile)),
            initialContents = mapOf(backupFile to "backup-content"),
        )
        coEvery { backupRepository.readBackupContentSerialized("backup-content") } returns BackupContent.Empty
        coEvery { backupRepository.import(BackupContent.Empty, triggerCloudBackup = false) } returns Unit

        val result = manager(provider).restore(provider.id, backupFile, null)

        assertTrue(result is CloudBackupResult.Success)
        coVerify(exactly = 1) { backupRepository.import(BackupContent.Empty, triggerCloudBackup = false) }
    }

    private fun manager(vararg providers: CloudBackupProvider) = CloudBackupManagerImpl(
        backupRepository = backupRepository,
        remoteBackupKeyPreference = keyPreference,
        settingsStore = settingsStore,
        providers = providers.toList(),
    )

    private fun file(name: String) = CloudBackupFile(
        providerId = CloudBackupProviderId("webdav"),
        remoteId = name,
        name = name,
    )

    private fun group(id: String, name: String) = BackupGroup(
        id = id,
        name = name,
        isExpanded = true,
    )

    private class FakeProvider(
        idValue: String,
        configured: Boolean = true,
        enabled: Boolean = true,
        initialFiles: List<CloudBackupFile> = emptyList(),
        initialContents: Map<String, String> = emptyMap(),
    ) : CloudBackupProvider {
        override val id = CloudBackupProviderId(idValue)
        private val providerState = CloudBackupProviderState(
            id = id,
            name = idValue,
            configured = configured,
            enabled = enabled,
        )
        val files = initialFiles.toMutableList()
        val uploaded = mutableListOf<Pair<String, String>>()
        val deleted = mutableListOf<String>()
        val contents = initialContents.toMutableMap()

        override fun state(): CloudBackupProviderState = providerState
        override fun observeState(): Flow<CloudBackupProviderState> = flowOf(providerState)

        override suspend fun upload(fileName: String, content: String): CloudBackupResult<Unit> {
            uploaded += fileName to content
            contents[fileName] = content
            files.removeAll { it.name == fileName }
            files += CloudBackupFile(id, fileName, fileName)
            return CloudBackupResult.Success(Unit)
        }

        override suspend fun listBackups(): CloudBackupResult<List<CloudBackupFile>> =
            CloudBackupResult.Success(files.toList())

        override suspend fun download(remoteId: String): CloudBackupResult<String> =
            contents[remoteId]
                ?.let { CloudBackupResult.Success(it) }
                ?: CloudBackupResult.Failure(CloudBackupError.FileNotFound)

        override suspend fun delete(remoteId: String): CloudBackupResult<Unit> {
            deleted += remoteId
            files.removeAll { it.remoteId == remoteId }
            contents.remove(remoteId)
            return CloudBackupResult.Success(Unit)
        }
    }
}
