package com.twofasapp.cloudbackup.core

import com.twofasapp.storage.PlainPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class CloudBackupSettingsStore(
    private val preferences: PlainPreferences,
) {
    companion object {
        const val DefaultMaxBackups = 10
        const val MinBackups = 1
        const val MaxBackups = 100
        private const val KeyMaxBackups = "cloudBackupMaxBackups"
        private const val KeyHistoryEnabled = "cloudBackupHistoryEnabled"
    }

    private val maxBackups = MutableStateFlow(readMaxBackups())
    private val historyEnabled = MutableStateFlow(preferences.getBoolean(KeyHistoryEnabled) ?: true)

    fun observeMaxBackups(): Flow<Int> = maxBackups

    fun getMaxBackups(): Int = maxBackups.value

    fun setMaxBackups(count: Int) {
        val normalized = count.coerceIn(MinBackups, MaxBackups)
        preferences.putInt(KeyMaxBackups, normalized)
        maxBackups.value = normalized
    }

    fun observeHistoryEnabled(): Flow<Boolean> = historyEnabled

    fun isHistoryEnabled(): Boolean = historyEnabled.value

    fun setHistoryEnabled(enabled: Boolean) {
        preferences.putBoolean(KeyHistoryEnabled, enabled)
        historyEnabled.value = enabled
    }

    private fun readMaxBackups(): Int = preferences.getInt(KeyMaxBackups)
        ?.coerceIn(MinBackups, MaxBackups)
        ?: DefaultMaxBackups
}
