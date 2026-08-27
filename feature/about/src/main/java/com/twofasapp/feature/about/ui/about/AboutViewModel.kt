package com.twofasapp.feature.about.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twofasapp.common.coroutines.Dispatchers
import com.twofasapp.common.environment.AppBuild
import com.twofasapp.data.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AboutViewModel(
    private val dispatchers: Dispatchers,
    private val appBuild: AppBuild,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val uiState = MutableStateFlow(AboutUiState())

    init {
        uiState.update {
            it.copy(versionName = appBuild.versionName)
        }
    }

    fun reviewDone() {
        viewModelScope.launch(dispatchers.io) {
            sessionRepository.setRateAppDisplayed(true)
        }
        // TODO
//        rateAppStatusPreference.put(
//            rateAppStatusPreference.get().copy(counterStarted = Instant.now(), counterReached = Instant.now())
//        )
    }

}
