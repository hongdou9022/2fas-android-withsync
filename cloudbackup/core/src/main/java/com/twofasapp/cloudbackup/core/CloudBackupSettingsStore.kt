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
    }

    private val maxBackups = MutableStateFlow(readMaxBackups())

    fun observeMaxBackups(): Flow<Int> = maxBackups

    fun getMaxBackups(): Int = maxBackups.value

    fun setMaxBackups(count: Int) {
        val normalized = count.coerceIn(MinBackups, MaxBackups)
        preferences.putInt(KeyMaxBackups, normalized)
        maxBackups.value = normalized
    }

    private fun readMaxBackups(): Int = preferences.getInt(KeyMaxBackups)
        ?.coerceIn(MinBackups, MaxBackups)
        ?: DefaultMaxBackups
}
