package com.twofasapp.cloudbackup.api

@JvmInline
value class CloudBackupProviderId(val value: String)

data class CloudBackupProviderState(
    val id: CloudBackupProviderId,
    val name: String,
    val configured: Boolean,
    val enabled: Boolean,
    val account: String? = null,
)

data class CloudBackupFile(
    val providerId: CloudBackupProviderId,
    val remoteId: String,
    val name: String,
    val size: Long? = null,
    val lastModifiedMillis: Long? = null,
)

enum class CloudBackupTrigger {
    ServicesChanged,
    Manual,
}

sealed interface CloudBackupError {
    data object NotConfigured : CloudBackupError
    data object NoEnabledProviders : CloudBackupError
    data object NetworkUnavailable : CloudBackupError
    data object Unauthorized : CloudBackupError
    data object FileNotFound : CloudBackupError
    data object InvalidBackup : CloudBackupError
    data object PasswordRequired : CloudBackupError
    data object WrongPassword : CloudBackupError
    data object Unknown : CloudBackupError
    data class Provider(val message: String?) : CloudBackupError
}

sealed interface CloudBackupResult<out T> {
    data class Success<T>(val value: T) : CloudBackupResult<T>
    data class Failure(val error: CloudBackupError) : CloudBackupResult<Nothing>
}

data class CloudBackupRunResult(
    val successfulProviders: List<CloudBackupProviderId>,
    val failedProviders: Map<CloudBackupProviderId, CloudBackupError>,
)

data class CloudBackupRestoreResult(
    val servicesCount: Int,
    val groupsCount: Int,
)

sealed interface CloudBackupOperationState {
    data object Idle : CloudBackupOperationState
    data class Running(val trigger: CloudBackupTrigger) : CloudBackupOperationState
    data class Completed(val result: CloudBackupRunResult) : CloudBackupOperationState
}
