package io.github.jan.supabase.realtime

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class PostgresActionData(
    val record: JsonObject? = null,
    @SerialName("old_record")
    val oldRecord: JsonObject? = null,
    val columns: List<Column>,
    @SerialName("commit_timestamp")
    val commitTimestamp: Instant
)