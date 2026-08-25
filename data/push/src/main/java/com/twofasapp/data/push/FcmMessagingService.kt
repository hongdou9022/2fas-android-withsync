package com.twofasapp.data.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.twofasapp.common.environment.AppBuild
import com.twofasapp.data.browserext.BrowserExtRepository
import com.twofasapp.data.push.domain.Push
import com.twofasapp.data.push.internal.PushFactory
import com.twofasapp.data.push.internal.PushLogger
import com.twofasapp.data.push.notification.ShowBrowserExtRequestNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class FcmMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val appBuild: AppBuild by inject()
    private val browserExtRepository: BrowserExtRepository by inject()
    private val pushRepository: PushRepository by inject()

    private val showBrowserExtensionRequest: ShowBrowserExtRequestNotification by inject()

    override fun onCreate() {
        super.onCreate()
        scope.launch(Dispatchers.IO) {
            pushRepository.observeNotificationPushes().flowOn(Dispatchers.IO).collect {
                when (it) {
                    is Push.BrowserExtRequest -> showBrowserExtensionRequest(it)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        if (appBuild.debuggable) {
            PushLogger.logMessage(remoteMessage)
        }

        PushFactory.createPush(remoteMessage)?.let { pushRepository.dispatchPush(it) }
    }

    override fun onNewToken(token: String) {
        if (appBuild.debuggable) {
            PushLogger.logToken(token)
        }

        scope.launch {
            runCatching { browserExtRepository.updateFcmToken(token) }
                .onFailure { Timber.e(it, "Failed to update browser extension FCM token") }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
