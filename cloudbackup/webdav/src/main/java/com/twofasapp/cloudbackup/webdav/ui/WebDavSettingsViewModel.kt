package com.twofasapp.cloudbackup.webdav.ui

import androidx.lifecycle.ViewModel
import com.twofasapp.cloudbackup.api.CloudBackupResult
import com.twofasapp.cloudbackup.webdav.WebDavConfig
import com.twofasapp.cloudbackup.webdav.WebDavRepository
import com.twofasapp.common.ktx.launchScoped
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class WebDavSettingsViewModel(
    private val repository: WebDavRepository,
) : ViewModel() {

    val uiState = MutableStateFlow(WebDavSettingsUiState())

    init {
        launchScoped {
            repository.observeConfig().collect { config ->
                uiState.update { it.copy(config = config) }
            }
        }
    }

    fun updateBaseUrl(value: String) = updateConfig { copy(baseUrl = value) }
    fun updateUsername(value: String) = updateConfig { copy(username = value) }
    fun updatePassword(value: String) = updateConfig { copy(password = value) }
    fun updateDirectory(value: String) = updateConfig { copy(remoteDirectory = value) }
    fun updateEnabled(value: Boolean) = updateConfig { copy(enabled = value) }

    fun testConnection() {
        val config = normalizedConfig() ?: return
        launchScoped {
            uiState.update { it.copy(testing = true) }
            when (val result = repository.testConnection(config)) {
                is CloudBackupResult.Success -> publishEvent(WebDavSettingsUiEvent.TestSuccess)
                is CloudBackupResult.Failure -> publishEvent(WebDavSettingsUiEvent.Failure(result.error))
            }
            uiState.update { it.copy(testing = false) }
        }
    }

    fun save() {
        val config = normalizedConfig() ?: return
        repository.save(config)
        publishEvent(WebDavSettingsUiEvent.Saved)
    }

    fun consumeEvent(event: WebDavSettingsUiEvent) {
        uiState.update { it.copy(events = it.events.minus(event)) }
    }

    private fun updateConfig(block: WebDavConfig.() -> WebDavConfig) {
        uiState.update { it.copy(config = it.config.block()) }
    }

    private fun normalizedConfig(): WebDavConfig? {
        val config = uiState.value.config.copy(
            baseUrl = uiState.value.config.baseUrl.trim().trimEnd('/'),
            username = uiState.value.config.username.trim(),
            remoteDirectory = uiState.value.config.remoteDirectory.trim().trim('/'),
        )
        if (config.configured.not()) {
            publishEvent(WebDavSettingsUiEvent.RequiredFields)
            return null
        }
        if (config.baseUrl.isHttpUrl().not()) {
            publishEvent(WebDavSettingsUiEvent.InvalidUrl)
            return null
        }
        return config
    }

    private fun publishEvent(event: WebDavSettingsUiEvent) {
        uiState.update { it.copy(events = it.events.plus(event)) }
    }

    private fun String.isHttpUrl(): Boolean = runCatching {
        val uri = URI(this)
        uri.scheme.lowercase() in setOf("http", "https") && uri.host.isNullOrBlank().not()
    }.getOrDefault(false)
}
