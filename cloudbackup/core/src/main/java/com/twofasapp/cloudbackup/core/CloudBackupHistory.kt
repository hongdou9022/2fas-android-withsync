package com.twofasapp.cloudbackup.core

import com.twofasapp.cloudbackup.api.CloudBackupTrigger
import com.twofasapp.data.services.domain.BackupContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

internal data class CloudBackupSnapshot(
    val services: List<CloudBackupSnapshotItem> = emptyList(),
    val groups: List<CloudBackupSnapshotItem> = emptyList(),
) {
    companion object {
        fun from(content: BackupContent): CloudBackupSnapshot = CloudBackupSnapshot(
            services = content.services.map { service ->
                val detail = service.otp.account?.takeIf { it.isNotBlank() }
                    ?: service.otp.issuer?.takeIf { it.isNotBlank() }
                CloudBackupSnapshotItem(
                    id = sha256(service.secret),
                    fingerprint = sha256(service.toString()),
                    label = listOfNotNull(service.name.takeIf { it.isNotBlank() }, detail).joinToString(" - "),
                )
            },
            groups = content.groups.map { group ->
                CloudBackupSnapshotItem(
                    id = group.id,
                    fingerprint = sha256(group.toString()),
                    label = group.name,
                )
            },
        )

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

internal data class CloudBackupSnapshotItem(
    val id: String,
    val fingerprint: String,
    val label: String,
)

internal data class CloudBackupHistoryEntry(
    val fileName: String,
    val createdAt: String,
    val trigger: String,
    val servicesTotal: Int,
    val groupsTotal: Int,
    val servicesAdded: List<String>,
    val servicesRemoved: List<String>,
    val servicesChanged: List<String>,
    val groupsAdded: List<String>,
    val groupsRemoved: List<String>,
    val groupsChanged: List<String>,
)

internal data class CloudBackupHistoryDocument(
    val entries: List<CloudBackupHistoryEntry> = emptyList(),
    val latestBackup: String? = null,
    val latestSnapshot: CloudBackupSnapshot = CloudBackupSnapshot(),
) {
    fun append(
        fileName: String,
        createdAt: String,
        trigger: CloudBackupTrigger,
        snapshot: CloudBackupSnapshot,
        retainedBackups: Set<String>,
    ): CloudBackupHistoryDocument {
        val serviceDelta = delta(latestSnapshot.services, snapshot.services)
        val groupDelta = delta(latestSnapshot.groups, snapshot.groups)
        val entry = CloudBackupHistoryEntry(
            fileName = fileName,
            createdAt = createdAt,
            trigger = trigger.name,
            servicesTotal = snapshot.services.size,
            groupsTotal = snapshot.groups.size,
            servicesAdded = serviceDelta.added,
            servicesRemoved = serviceDelta.removed,
            servicesChanged = serviceDelta.changed,
            groupsAdded = groupDelta.added,
            groupsRemoved = groupDelta.removed,
            groupsChanged = groupDelta.changed,
        )
        return copy(
            entries = entries
                .filter { it.fileName in retainedBackups && it.fileName != fileName }
                .plus(entry),
            latestBackup = fileName,
            latestSnapshot = snapshot,
        )
    }

    fun remove(fileNames: Set<String>): CloudBackupHistoryDocument = copy(
        entries = entries.filterNot { it.fileName in fileNames },
        latestBackup = latestBackup?.takeUnless { it in fileNames },
        latestSnapshot = latestSnapshot.takeUnless { latestBackup in fileNames } ?: CloudBackupSnapshot(),
    )

    private fun delta(
        previous: List<CloudBackupSnapshotItem>,
        current: List<CloudBackupSnapshotItem>,
    ): Delta {
        val previousById = previous.associateBy { it.id }
        val currentById = current.associateBy { it.id }
        return Delta(
            added = (currentById.keys - previousById.keys).mapNotNull { currentById[it]?.label }.sortedLabels(),
            removed = (previousById.keys - currentById.keys).mapNotNull { previousById[it]?.label }.sortedLabels(),
            changed = (previousById.keys intersect currentById.keys)
                .filter { previousById[it]?.fingerprint != currentById[it]?.fingerprint }
                .mapNotNull { currentById[it]?.label }
                .sortedLabels(),
        )
    }

    private fun List<String>.sortedLabels() = sortedWith(String.CASE_INSENSITIVE_ORDER)

    private data class Delta(
        val added: List<String>,
        val removed: List<String>,
        val changed: List<String>,
    )
}

internal object CloudBackupHistoryCodec {
    private const val Version = 1
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun encode(document: CloudBackupHistoryDocument): String = json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("version", JsonPrimitive(Version))
            put("latestBackup", document.latestBackup?.let(::JsonPrimitive) ?: JsonNull)
            put("latestSnapshot", document.latestSnapshot.toJson())
            put("entries", buildJsonArray { document.entries.forEach { add(it.toJson()) } })
        },
    )

    fun decode(content: String): CloudBackupHistoryDocument {
        val root = json.parseToJsonElement(content).jsonObject
        return CloudBackupHistoryDocument(
            entries = root.array("entries").map { it.jsonObject.toHistoryEntry() },
            latestBackup = root.string("latestBackup"),
            latestSnapshot = root["latestSnapshot"]?.jsonObject?.toSnapshot() ?: CloudBackupSnapshot(),
        )
    }

    private fun CloudBackupSnapshot.toJson() = buildJsonObject {
        put("services", services.toJson())
        put("groups", groups.toJson())
    }

    private fun List<CloudBackupSnapshotItem>.toJson() = buildJsonArray {
        forEach { item ->
            add(
                buildJsonObject {
                    put("id", JsonPrimitive(item.id))
                    put("fingerprint", JsonPrimitive(item.fingerprint))
                    put("label", JsonPrimitive(item.label))
                },
            )
        }
    }

    private fun CloudBackupHistoryEntry.toJson() = buildJsonObject {
        put("fileName", JsonPrimitive(fileName))
        put("createdAt", JsonPrimitive(createdAt))
        put("trigger", JsonPrimitive(trigger))
        put("servicesTotal", JsonPrimitive(servicesTotal))
        put("groupsTotal", JsonPrimitive(groupsTotal))
        put("servicesAdded", servicesAdded.toJsonArray())
        put("servicesRemoved", servicesRemoved.toJsonArray())
        put("servicesChanged", servicesChanged.toJsonArray())
        put("groupsAdded", groupsAdded.toJsonArray())
        put("groupsRemoved", groupsRemoved.toJsonArray())
        put("groupsChanged", groupsChanged.toJsonArray())
    }

    private fun List<String>.toJsonArray() = buildJsonArray { forEach { add(JsonPrimitive(it)) } }

    private fun JsonObject.toSnapshot() = CloudBackupSnapshot(
        services = array("services").map { it.jsonObject.toSnapshotItem() },
        groups = array("groups").map { it.jsonObject.toSnapshotItem() },
    )

    private fun JsonObject.toSnapshotItem() = CloudBackupSnapshotItem(
        id = string("id").orEmpty(),
        fingerprint = string("fingerprint").orEmpty(),
        label = string("label").orEmpty(),
    )

    private fun JsonObject.toHistoryEntry() = CloudBackupHistoryEntry(
        fileName = string("fileName").orEmpty(),
        createdAt = string("createdAt").orEmpty(),
        trigger = string("trigger").orEmpty(),
        servicesTotal = int("servicesTotal"),
        groupsTotal = int("groupsTotal"),
        servicesAdded = strings("servicesAdded"),
        servicesRemoved = strings("servicesRemoved"),
        servicesChanged = strings("servicesChanged"),
        groupsAdded = strings("groupsAdded"),
        groupsRemoved = strings("groupsRemoved"),
        groupsChanged = strings("groupsChanged"),
    )

    private fun JsonObject.string(key: String) = get(key)?.jsonPrimitive?.contentOrNull
    private fun JsonObject.int(key: String) = get(key)?.jsonPrimitive?.intOrNull ?: 0
    private fun JsonObject.array(key: String): JsonArray = get(key)?.jsonArray ?: JsonArray(emptyList())
    private fun JsonObject.strings(key: String) = array(key).mapNotNull { it.jsonPrimitive.contentOrNull }
}
