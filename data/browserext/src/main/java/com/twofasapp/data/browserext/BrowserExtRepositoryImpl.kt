package com.twofasapp.data.browserext

import com.google.firebase.messaging.FirebaseMessaging
import com.twofasapp.common.coroutines.Dispatchers
import com.twofasapp.common.ktx.decodeBase64ToByteArray
import com.twofasapp.common.ktx.encodeBase64ToString
import com.twofasapp.data.browserext.domain.MobileDevice
import com.twofasapp.data.browserext.domain.PairedBrowser
import com.twofasapp.data.browserext.domain.TokenRequest
import com.twofasapp.data.browserext.local.BrowserExtLocalSource
import com.twofasapp.data.browserext.mapper.asDomain
import com.twofasapp.data.browserext.remote.BrowserExtRemoteSource
import com.twofasapp.data.browserext.remote.model.ApproveLoginRequestBody
import com.twofasapp.data.browserext.remote.model.PairBrowserBody
import com.twofasapp.data.browserext.remote.model.RegisterDeviceBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

internal class BrowserExtRepositoryImpl(
    private val dispatchers: Dispatchers,
    private val localSource: BrowserExtLocalSource,
    private val remoteSource: BrowserExtRemoteSource,
) : BrowserExtRepository {

    companion object {
        private const val PLATFORM = "android"
        private const val FCM_TOKEN_TIMEOUT_MS = 15_000L
    }

    override fun observeMobileDevice(): Flow<MobileDevice> {
        return localSource.observeMobileDevice()
    }

    override fun observePairedBrowsers(): Flow<List<PairedBrowser>> {
        return localSource.observePairedBrowsers()
    }

    override suspend fun updateMobileDevice(mobileDevice: MobileDevice) {
        remoteSource.updateMobileDevice(mobileDevice)
        localSource.saveMobileDevice(mobileDevice)
    }

    override suspend fun refreshFcmToken() {
        try {
            updateFcmToken(getFcmToken())
            localSource.saveFcmSyncStatus("success")
        } catch (e: Exception) {
            localSource.saveFcmSyncStatus("${e.javaClass.name}: ${e.message.orEmpty()}")
            throw e
        }
    }

    override suspend fun updateFcmToken(fcmToken: String) {
        check(fcmToken.isNotBlank()) { "FCM token is empty" }

        val mobileDevice = localSource.observeMobileDevice().first()
        if (mobileDevice.id.isBlank() || mobileDevice.fcmToken == fcmToken) return

        updateMobileDevice(mobileDevice.copy(fcmToken = fcmToken))
    }

    override suspend fun registerMobileDevice(deviceName: String, devicePublicKey: String, fcmToken: String): MobileDevice {
        val mobileDevice = remoteSource.registerMobileDevice(
            devicePublicKey = devicePublicKey,
            body = RegisterDeviceBody(
                name = deviceName,
                fcm_token = fcmToken,
                platform = PLATFORM,
            ),
        )

        localSource.saveMobileDevice(mobileDevice)
        return mobileDevice
    }

    override suspend fun pairBrowser(deviceId: String, extensionId: String, deviceName: String, devicePublicKey: String): PairedBrowser {
        val browser = remoteSource.pairBrowser(
            deviceId = deviceId,
            body = PairBrowserBody(
                extension_id = extensionId,
                device_name = deviceName,
                device_public_key = devicePublicKey,
            ),
        )
        localSource.savePairedBrowser(browser)
        return browser
    }

    override suspend fun getPairedBrowser(id: String): PairedBrowser {
        localSource.observePairedBrowsers().first().firstOrNull { it.id == id }?.let { return it }

        fetchPairedBrowsers()

        return localSource.observePairedBrowsers().first().first { it.id == id }
    }

    override suspend fun updatePairedBrowser(extensionId: String, newName: String) {
        remoteSource.updateBrowserName(extensionId = extensionId, newName = newName)
        fetchPairedBrowsers()
    }

    override suspend fun fetchPairedBrowsers() {
        withContext(dispatchers.io) {
            val id = localSource.observeMobileDevice().first().id

            if (id.isNotBlank()) {
                localSource.updatePairedBrowsers(remoteSource.getBrowsers(id).map { it.asDomain() })
            }
        }
    }

    override suspend fun getMobileDevice(): MobileDevice {
        return localSource.observeMobileDevice().first()
    }

    override fun observeTokenRequests(): Flow<List<TokenRequest>> {
        return localSource.observeTokenRequests()
    }

    override suspend fun fetchTokenRequests() {
        localSource.updateTokenRequests(pullTokenRequests())
    }

    override suspend fun pullTokenRequests(): List<TokenRequest> {
        return withContext(dispatchers.io) {
            val deviceId = observeMobileDevice().firstOrNull()?.id
            if (deviceId.isNullOrBlank()) {
                localSource.saveRequestFetchStatus("no_device")
                return@withContext emptyList()
            }

            try {
                remoteSource.fetchTokenRequests(deviceId)
                    .map { it.asDomain() }
                    .also { localSource.saveRequestFetchStatus("success:${it.size}") }
            } catch (e: Exception) {
                localSource.saveRequestFetchStatus("error:${e.javaClass.name}")
                throw e
            }
        }
    }

    override suspend fun getFcmToken(): String {
        return withTimeoutOrNull(FCM_TOKEN_TIMEOUT_MS) {
            FirebaseMessaging.getInstance().token.await()
        }.orEmpty()
    }

    override suspend fun deleteTokenRequest(requestId: String) {
        withContext(dispatchers.io) {
            localSource.deleteTokenRequest(requestId)
        }
    }

    override suspend fun deletePairedBrowser(deviceId: String, extensionId: String) {
        remoteSource.deletePairedBrowser(deviceId, extensionId)
        fetchPairedBrowsers()
    }

    override suspend fun acceptLoginRequest(deviceId: String, extensionId: String, requestId: String, codeUnencrypted: String) {
        val extension = getPairedBrowser(extensionId)
        val codeEncrypted = encryptCode(
            code = codeUnencrypted,
            extensionPublicKey = extension.extensionPublicKey,
        )

        return remoteSource.acceptLoginRequest(
            deviceId = deviceId,
            body = ApproveLoginRequestBody(
                extension_id = extensionId,
                token_request_id = requestId,
                token = codeEncrypted,
            ),
        )
    }

    override suspend fun denyLoginRequest(extensionId: String, requestId: String) {
        return remoteSource.denyLoginRequest(extensionId, requestId)
    }

    private suspend fun encryptCode(
        code: String,
        extensionPublicKey: String,
    ): String {
        val publicKeySpec = X509EncodedKeySpec(extensionPublicKey.decodeBase64ToByteArray())
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(publicKeySpec)

        val cipher: Cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
            .apply {
                // To use SHA-256 the main digest and SHA-1 as the MGF1 digest
                init(
                    Cipher.ENCRYPT_MODE,
                    publicKey,
                    OAEPParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT),
                )
                // To use SHA-512 for both digests
                // init(Cipher.ENCRYPT_MODE, publicKey, OAEPParameterSpec("SHA-510", "MGF1", MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT))
            }
        val bytes = cipher.doFinal(code.toByteArray())
        return bytes.encodeBase64ToString()
    }
}
