package com.twofasapp.cloudbackup.webdav

import com.twofasapp.storage.EncryptedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class WebDavConfigStore(
    private val preferences: EncryptedPreferences,
) {
    companion object {
        private const val Key = "cloudBackupWebDavConfig"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
    private val config = MutableStateFlow(read())

    fun get(): WebDavConfig = config.value

    fun observe(): Flow<WebDavConfig> = config

    fun save(value: WebDavConfig) {
        preferences.putString(Key, json.encodeToString(value))
        config.value = value
    }

    private fun read(): WebDavConfig = preferences.getString(Key)
        ?.let { runCatching { json.decodeFromString<WebDavConfig>(it) }.getOrNull() }
        ?: WebDavConfig()
}
