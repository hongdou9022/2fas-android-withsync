package com.twofasapp.base

import com.twofasapp.prefs.model.CheckLockStatus
import com.twofasapp.prefs.model.LockMethodEntity
import com.twofasapp.prefs.usecase.SkipAppUnlockPreference
import java.time.Instant
import javax.inject.Provider

class AuthTracker(
    private val checkLockStatus: Provider<CheckLockStatus>,
    private val skipAppUnlockPreference: Provider<SkipAppUnlockPreference>,
) {

    companion object {
        private const val VALIDITY_TIME_MS = 30 * 1000L
    }

    private var lastBackgroundTime: Instant = Instant.MIN
    private var lastForegroundTime: Instant = Instant.now()
    private var isAuthenticated = false
    private var forceAuthentication = false

    fun onAppCreate() {
        reset(forceAuthentication = false)
    }

    fun onSplashScreen() {
        reset(forceAuthentication = false)
    }

    fun onBrowserExtRequest() {
        reset(forceAuthentication = true)
    }

    fun onAuthenticateScreen() {
        reset(forceAuthentication = true)
    }

    fun onWidgetSettingsScreen() {
        reset(forceAuthentication = true)
    }

    fun onChangingLockStatus() {
        isAuthenticated = true
        forceAuthentication = false
    }

    fun onMovingToBackground() {
        if (isAuthenticated) {
            lastBackgroundTime = Instant.now()
            isAuthenticated = false
        }
    }

    fun onMovingToForeground() {
        lastForegroundTime = Instant.now()

        if (isValidityTimeElapsed().not()) {
            isAuthenticated = true
        }
    }

    fun onAuthenticated() {
        isAuthenticated = true
        forceAuthentication = false
    }

    fun shouldAuthenticate(): AuthenticationStatus {
        return when {
            isNoLock() -> {
                AuthenticationStatus.Valid
            }
            isSessionStillAuthenticated() -> {
                AuthenticationStatus.Valid
            }
            forceAuthentication -> {
                AuthenticationStatus.Expired
            }
            shouldSkipAppUnlock() -> {
                AuthenticationStatus.Valid
            }
            isValidityTimeElapsed() -> {
                AuthenticationStatus.Expired
            }
            else -> {
                AuthenticationStatus.Valid
            }
        }
    }

    private fun isSessionStillAuthenticated() = isAuthenticated

    private fun isNoLock() = checkLockStatus.get().execute() == LockMethodEntity.NO_LOCK

    private fun shouldSkipAppUnlock() = skipAppUnlockPreference.get().get()

    private fun isValidityTimeElapsed() = lastForegroundTime.minusMillis(VALIDITY_TIME_MS).isAfter(lastBackgroundTime)

    private fun reset(forceAuthentication: Boolean) {
        lastBackgroundTime = Instant.MIN
        lastForegroundTime = Instant.now()
        isAuthenticated = false
        this.forceAuthentication = forceAuthentication
    }
}
