package com.twofasapp.cloudbackup.webdav

import kotlinx.serialization.Serializable

@Serializable
data class WebDavConfig(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val remoteDirectory: String = "2FAS",
    val enabled: Boolean = false,
) {
    val configured: Boolean
        get() = baseUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}
