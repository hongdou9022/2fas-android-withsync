package com.twofasapp.cloudbackup.webdav

import com.twofasapp.cloudbackup.api.CloudBackupError
import com.twofasapp.cloudbackup.api.CloudBackupFile
import com.twofasapp.cloudbackup.api.CloudBackupProviderId
import com.twofasapp.cloudbackup.api.CloudBackupResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

internal class WebDavClient {
    private val httpClient = HttpClient(OkHttp) {
        expectSuccess = false
        followRedirects = true
    }

    suspend fun testConnection(config: WebDavConfig): CloudBackupResult<Unit> {
        if (config.configured.not()) return CloudBackupResult.Failure(CloudBackupError.NotConfigured)
        return when (val directory = ensureDirectory(config)) {
            is CloudBackupResult.Failure -> directory
            is CloudBackupResult.Success -> when (val list = list(config, CloudBackupProviderId("webdav"))) {
                is CloudBackupResult.Failure -> list
                is CloudBackupResult.Success -> CloudBackupResult.Success(Unit)
            }
        }
    }

    suspend fun upload(config: WebDavConfig, fileName: String, content: String): CloudBackupResult<Unit> {
        if (config.configured.not()) return CloudBackupResult.Failure(CloudBackupError.NotConfigured)
        return execute {
            when (val directory = ensureDirectory(config)) {
                is CloudBackupResult.Failure -> return@execute directory
                is CloudBackupResult.Success -> Unit
            }

            val finalUrl = fileUrl(config, fileName)
            val temporaryUrl = fileUrl(config, ".$fileName.uploading-${UUID.randomUUID()}")
            val putResponse = httpClient.put(temporaryUrl) {
                authorize(config)
                contentType(ContentType.Application.Json)
                setBody(content)
            }
            if (putResponse.status.isSuccessful().not()) return@execute putResponse.asFailure()

            val moveResponse = httpClient.request(temporaryUrl) {
                method = HttpMethod("MOVE")
                authorize(config)
                header("Destination", finalUrl)
                header("Overwrite", "T")
            }
            if (moveResponse.status.isSuccessful()) {
                CloudBackupResult.Success(Unit)
            } else {
                val fallback = httpClient.put(finalUrl) {
                    authorize(config)
                    contentType(ContentType.Application.Json)
                    setBody(content)
                }
                httpClient.delete(temporaryUrl) { authorize(config) }
                if (fallback.status.isSuccessful()) CloudBackupResult.Success(Unit) else fallback.asFailure()
            }
        }
    }

    suspend fun list(
        config: WebDavConfig,
        providerId: CloudBackupProviderId,
    ): CloudBackupResult<List<CloudBackupFile>> {
        if (config.configured.not()) return CloudBackupResult.Failure(CloudBackupError.NotConfigured)
        return execute {
            when (val directory = ensureDirectory(config)) {
                is CloudBackupResult.Failure -> return@execute directory
                is CloudBackupResult.Success -> Unit
            }

            val response = httpClient.request(directoryUrl(config)) {
                method = HttpMethod("PROPFIND")
                authorize(config)
                header("Depth", "1")
                contentType(ContentType.Application.Xml)
                setBody(PropFindBody)
            }
            if (response.status != HttpStatusCode.MultiStatus && response.status.isSuccessful().not()) {
                return@execute response.asFailure()
            }

            CloudBackupResult.Success(parseFiles(response.bodyAsText(), directoryUrl(config), providerId))
        }
    }

    suspend fun download(config: WebDavConfig, remoteId: String): CloudBackupResult<String> {
        if (config.configured.not()) return CloudBackupResult.Failure(CloudBackupError.NotConfigured)
        return execute {
            val response = httpClient.get(remoteId) { authorize(config) }
            if (response.status.isSuccessful()) {
                CloudBackupResult.Success(response.bodyAsText())
            } else {
                response.asFailure()
            }
        }
    }

    suspend fun delete(config: WebDavConfig, remoteId: String): CloudBackupResult<Unit> {
        if (config.configured.not()) return CloudBackupResult.Failure(CloudBackupError.NotConfigured)
        return execute {
            val response = httpClient.delete(remoteId) { authorize(config) }
            if (response.status.isSuccessful() || response.status == HttpStatusCode.NotFound) {
                CloudBackupResult.Success(Unit)
            } else {
                response.asFailure()
            }
        }
    }

    private suspend fun ensureDirectory(config: WebDavConfig): CloudBackupResult<Unit> {
        var currentUrl = config.baseUrl.trimEnd('/')
        val segments = config.remoteDirectory.trim('/').split('/').filter { it.isNotBlank() }

        for (segment in segments) {
            currentUrl += "/${segment.encodeURLPathPart()}"
            val probe = httpClient.request(currentUrl) {
                method = HttpMethod("PROPFIND")
                authorize(config)
                header("Depth", "0")
                contentType(ContentType.Application.Xml)
                setBody(PropFindBody)
            }
            if (probe.status.isSuccessful() || probe.status == HttpStatusCode.MultiStatus) continue
            if (probe.status != HttpStatusCode.NotFound) return probe.asFailure()

            val create = httpClient.request(currentUrl) {
                method = HttpMethod("MKCOL")
                authorize(config)
            }
            if (create.status.isSuccessful().not() && create.status != HttpStatusCode.MethodNotAllowed) {
                return create.asFailure()
            }
        }

        return CloudBackupResult.Success(Unit)
    }

    private fun directoryUrl(config: WebDavConfig): String {
        val directory = config.remoteDirectory.trim('/').split('/').filter { it.isNotBlank() }
            .joinToString("/") { it.encodeURLPathPart() }
        return if (directory.isBlank()) config.baseUrl.trimEnd('/') else "${config.baseUrl.trimEnd('/')}/$directory"
    }

    private fun fileUrl(config: WebDavConfig, fileName: String): String =
        "${directoryUrl(config).trimEnd('/')}/${fileName.encodeURLPathPart()}"

    private fun io.ktor.client.request.HttpRequestBuilder.authorize(config: WebDavConfig) {
        val credentials = Base64.getEncoder()
            .encodeToString("${config.username}:${config.password}".toByteArray(StandardCharsets.UTF_8))
        header(HttpHeaders.Authorization, "Basic $credentials")
    }

    private suspend fun <T> execute(block: suspend () -> CloudBackupResult<T>): CloudBackupResult<T> {
        return try {
            block()
        } catch (e: IOException) {
            CloudBackupResult.Failure(CloudBackupError.NetworkUnavailable)
        } catch (e: Exception) {
            CloudBackupResult.Failure(CloudBackupError.Provider(e.message))
        }
    }

    private suspend fun HttpResponse.asFailure(): CloudBackupResult.Failure = when (status) {
        HttpStatusCode.Unauthorized,
        HttpStatusCode.Forbidden,
        -> CloudBackupResult.Failure(CloudBackupError.Unauthorized)

        HttpStatusCode.NotFound -> CloudBackupResult.Failure(CloudBackupError.FileNotFound)
        else -> CloudBackupResult.Failure(CloudBackupError.Provider("HTTP ${status.value}: ${bodyAsText().take(256)}"))
    }

    private fun HttpStatusCode.isSuccessful(): Boolean = value in 200..299

    private fun parseFiles(
        xml: String,
        baseUrl: String,
        providerId: CloudBackupProviderId,
    ): List<CloudBackupFile> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { isXIncludeAware = false }
            runCatching { isExpandEntityReferences = false }
            runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
        }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        val responses = document.getElementsByTagNameNS("*", "response")
        val files = mutableListOf<CloudBackupFile>()
        val directoryUri = URI(baseUrl.trimEnd('/') + "/").normalize()

        for (index in 0 until responses.length) {
            val element = responses.item(index) as? Element ?: continue
            if (element.getElementsByTagNameNS("*", "collection").length > 0) continue
            val href = element.firstText("href") ?: continue
            val absoluteUri = directoryUri.resolve(href).normalize()
            if (absoluteUri.isInside(directoryUri).not()) continue
            val absoluteUrl = absoluteUri.toString()
            val encodedName = absoluteUri.path.substringAfterLast('/')
            val name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.name())
            if (name.isBlank()) continue

            files += CloudBackupFile(
                providerId = providerId,
                remoteId = absoluteUrl,
                name = name,
                size = element.firstText("getcontentlength")?.toLongOrNull(),
                lastModifiedMillis = element.firstText("getlastmodified")?.let {
                    runCatching {
                        ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
                    }.getOrNull()
                },
            )
        }

        return files
    }

    private fun Element.firstText(localName: String): String? =
        getElementsByTagNameNS("*", localName).item(0)?.textContent?.trim()?.takeIf { it.isNotBlank() }

    private fun URI.isInside(directory: URI): Boolean =
        scheme.equals(directory.scheme, ignoreCase = true) &&
            host.equals(directory.host, ignoreCase = true) &&
            effectivePort() == directory.effectivePort() &&
            path.startsWith(directory.path)

    private fun URI.effectivePort(): Int = when {
        port >= 0 -> port
        scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }

    private companion object {
        const val PropFindBody = """<?xml version="1.0" encoding="utf-8"?>
            <d:propfind xmlns:d="DAV:">
              <d:prop>
                <d:resourcetype/>
                <d:getlastmodified/>
                <d:getcontentlength/>
              </d:prop>
            </d:propfind>"""
    }
}
