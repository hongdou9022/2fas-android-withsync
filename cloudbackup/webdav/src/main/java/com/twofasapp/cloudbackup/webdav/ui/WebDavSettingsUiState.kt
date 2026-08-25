package com.twofasapp.cloudbackup.webdav.ui

import com.twofasapp.cloudbackup.api.CloudBackupError
import com.twofasapp.cloudbackup.webdav.WebDavConfig

internal data class WebDavSettingsUiState(
    val config: WebDavConfig = WebDavConfig(),
    val testing: Boolean = false,
    val events: List<WebDavSettingsUiEvent> = emptyList(),
)

internal sealed interface WebDavSettingsUiEvent {
    data object TestSuccess : WebDavSettingsUiEvent
    data object Saved : WebDavSettingsUiEvent
    data object RequiredFields : WebDavSettingsUiEvent
    data object InvalidUrl : WebDavSettingsUiEvent
    data class Failure(val error: CloudBackupError) : WebDavSettingsUiEvent
}
