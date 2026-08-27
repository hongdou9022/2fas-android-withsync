package com.twofasapp.feature.home.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twofasapp.data.session.SettingsRepository
import com.twofasapp.data.session.domain.HomeUiMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class SettingsViewModel(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val homeUiMode = settingsRepository.observeAppSettings()
        .map { it.homeUiMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = settingsRepository.getAppSettings().homeUiMode,
        )
}
