package com.twofasapp.feature.backup.ui.cloudbackup

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twofasapp.cloudbackup.api.CloudBackupFile
import com.twofasapp.core.design.MdtIcons
import com.twofasapp.core.design.feature.settings.SettingsLink
import com.twofasapp.core.design.foundation.button.IconButton
import com.twofasapp.core.design.foundation.dialog.ConfirmDialog
import com.twofasapp.core.design.foundation.dialog.PasswordDialog
import com.twofasapp.core.design.foundation.progress.CircularProgressIndicator
import com.twofasapp.core.design.foundation.topbar.TopAppBar
import com.twofasapp.core.design.ktx.strings
import com.twofasapp.core.design.ktx.toastShort
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CloudBackupFilesScreen(
    viewModel: CloudBackupFilesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CloudBackupFilesContent(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onRestore = viewModel::restore,
        onDelete = viewModel::delete,
        onEventConsumed = viewModel::consumeEvent,
    )
}

@Composable
private fun CloudBackupFilesContent(
    uiState: CloudBackupFilesUiState,
    onRefresh: () -> Unit = {},
    onRestore: (CloudBackupFile, String?) -> Unit = { _, _ -> },
    onDelete: (CloudBackupFile) -> Unit = {},
    onEventConsumed: (CloudBackupFilesUiEvent) -> Unit = {},
) {
    val context = LocalContext.current
    val strings = LocalContext.strings
    var restoreFile by remember { mutableStateOf<CloudBackupFile?>(null) }
    var deleteFile by remember { mutableStateOf<CloudBackupFile?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    uiState.events.firstOrNull()?.let { event ->
        LaunchedEffect(event) {
            when (event) {
                is CloudBackupFilesUiEvent.RequestPassword -> {
                    restoreFile = event.file
                    passwordError = event.wrongPassword
                    showPassword = true
                }

                CloudBackupFilesUiEvent.RestoreSuccess -> context.toastShort(strings.cloudBackupRestoreSuccess)
                CloudBackupFilesUiEvent.DeleteSuccess -> context.toastShort(strings.cloudBackupDeleteSuccess)
                CloudBackupFilesUiEvent.DeleteFailed -> context.toastShort(strings.cloudBackupDeleteFailed)
                CloudBackupFilesUiEvent.ListFailed -> context.toastShort(strings.cloudBackupFilesLoadFailed)
                CloudBackupFilesUiEvent.OperationFailed -> context.toastShort(strings.cloudBackupRestoreFailed)
            }
            onEventConsumed(event)
        }
    }

    Scaffold(
        topBar = { TopAppBar(titleText = uiState.providerName.ifBlank { strings.cloudBackupFilesTitle }) },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            when {
                uiState.loading -> item {
                    SettingsLink(
                        title = strings.cloudBackupFilesLoading,
                        showEmptySpaceWhenNoIcon = false,
                        endContent = { CircularProgressIndicator() },
                    )
                }

                uiState.files.isEmpty() -> item {
                    SettingsLink(
                        title = strings.cloudBackupFilesEmpty,
                        icon = MdtIcons.Refresh,
                        onClick = onRefresh,
                    )
                }

                else -> uiState.files.forEach { file ->
                    item {
                        val busy = uiState.restoringRemoteId != null || uiState.deletingRemoteId != null
                        SettingsLink(
                            title = file.name,
                            subtitle = file.details(),
                            icon = MdtIcons.Download,
                            enabled = busy.not(),
                            endContent = {
                                if (
                                    uiState.restoringRemoteId == file.remoteId ||
                                    uiState.deletingRemoteId == file.remoteId
                                ) {
                                    CircularProgressIndicator()
                                } else {
                                    IconButton(
                                        painter = MdtIcons.Delete,
                                        contentDescription = strings.cloudBackupDelete,
                                        enabled = busy.not(),
                                        onClick = {
                                            deleteFile = file
                                            showDeleteConfirmation = true
                                        },
                                    )
                                }
                            },
                            onClick = {
                                restoreFile = file
                                showRestoreConfirmation = true
                            },
                        )
                    }
                }
            }
        }
    }

    if (showRestoreConfirmation) {
        ConfirmDialog(
            onDismissRequest = { showRestoreConfirmation = false },
            title = strings.cloudBackupRestoreTitle,
            body = strings.cloudBackupRestoreMessage,
            positive = strings.commonContinue,
            negative = strings.commonCancel,
            onPositive = {
                showRestoreConfirmation = false
                restoreFile?.let { onRestore(it, null) }
            },
        )
    }

    if (showDeleteConfirmation) {
        ConfirmDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = strings.cloudBackupDeleteTitle,
            body = strings.cloudBackupDeleteMessage,
            positive = strings.cloudBackupDelete,
            negative = strings.commonCancel,
            onPositive = {
                showDeleteConfirmation = false
                deleteFile?.let(onDelete)
            },
        )
    }

    if (showPassword) {
        PasswordDialog(
            onDismissRequest = {
                showPassword = false
                passwordError = false
            },
            confirmRequired = false,
            title = strings.backupEnterPassword,
            body = strings.cloudBackupRestorePassword,
            error = strings.cloudBackupRestorePasswordWrong.takeIf { passwordError },
            positive = strings.commonContinue,
            onPositive = { password ->
                showPassword = false
                passwordError = false
                restoreFile?.let { onRestore(it, password) }
            },
        )
    }
}

private val FileTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

private fun CloudBackupFile.details(): String {
    val time = lastModifiedMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(FileTimeFormatter)
    }
    val formattedSize = size?.let {
        when {
            it >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", it / (1024.0 * 1024.0))
            it >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", it / 1024.0)
            else -> "$it B"
        }
    }
    return listOfNotNull(time, formattedSize).joinToString("\n")
}
