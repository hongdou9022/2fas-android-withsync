package com.twofasapp.feature.backup.ui.export

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.twofasapp.common.environment.AppBuild
import com.twofasapp.common.ktx.launchScoped
import com.twofasapp.common.ktx.runSafely
import com.twofasapp.data.services.BackupRepository
import com.twofasapp.data.services.domain.CloudSyncTrigger
import com.twofasapp.data.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

internal class BackupExportViewModel(
    private val context: Application,
    private val appBuild: AppBuild,
    private val backupRepository: BackupRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState: MutableStateFlow<BackupExportUiState> = MutableStateFlow(BackupExportUiState())
    private var passwordStateInitialized = false

    init {
        launchScoped {
            backupRepository.observeBackupPasswordSet().collect { passwordSet ->
                uiState.update {
                    it.copy(
                        passwordSet = passwordSet,
                        passwordChecked = when {
                            passwordSet.not() -> false
                            passwordStateInitialized.not() -> true
                            else -> it.passwordChecked
                        },
                    )
                }
                passwordStateInitialized = true
            }
        }
    }

    fun togglePassword() {
        val state = uiState.value
        when {
            state.passwordChecked -> uiState.update { it.copy(passwordChecked = false) }
            state.passwordSet -> uiState.update { it.copy(passwordChecked = true) }
            else -> publishEvent(BackupExportUiEvent.ShowSetPasswordDialog)
        }
    }

    fun setPassword(password: String) {
        launchScoped {
            backupRepository.setBackupPassword(password)
            uiState.update { it.copy(passwordSet = true, passwordChecked = true) }

            if (backupRepository.observeCloudBackupStatus().first().active) {
                backupRepository.dispatchCloudSync(CloudSyncTrigger.SetPassword, password)
            }
        }
    }

    fun shareBackup() {
        launchScoped {
            runSafely {
                createBackupContent()
            }
                .onSuccess { content ->
                    sessionRepository.resetBackupReminder()

                    publishEvent(
                        BackupExportUiEvent.ShowSharePicker(
                            appId = appBuild.id,
                            content = content,
                        ),
                    )
                }
                .onFailure { publishEvent(BackupExportUiEvent.ShareError) }
        }
    }

    fun downloadBackup(fileUri: Uri) {
        launchScoped {
            runSafely {
                val content = createBackupContent()

                context.contentResolver.openOutputStream(fileUri)
                    ?.use { outputStream ->
                        outputStream.write(content.toByteArray(Charsets.UTF_8))
                    }
            }
                .onSuccess {
                    sessionRepository.resetBackupReminder()
                    publishEvent(BackupExportUiEvent.DownloadSuccess)
                }
                .onFailure { publishEvent(BackupExportUiEvent.DownloadError) }
        }
    }

    fun consumeEvent(event: BackupExportUiEvent) {
        uiState.update { it.copy(events = it.events.minus(event)) }
    }

    private fun publishEvent(event: BackupExportUiEvent) {
        uiState.update { it.copy(events = it.events.plus(event)) }
    }

    private suspend fun createBackupContent(): String =
        if (uiState.value.passwordChecked) {
            backupRepository.createBackupContentSerializedWithBackupKey()
        } else {
            backupRepository.createBackupContentSerialized()
        }
}
