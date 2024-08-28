package io.github.jan.supabase.realtime.event

import io.github.jan.supabase.logging.d
import io.github.jan.supabase.realtime.PostgresJoinConfig
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.RealtimeMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Event that handles the server changes
 */
data object RPostgresServerChangesEvent : RealtimeEvent {

    override suspend fun handle(channel: RealtimeChannel, message: RealtimeMessage) {
        val serverPostgresChanges = message.payload["response"]?.jsonObject?.get("postgres_changes")?.jsonArray?.let { Json.decodeFromJsonElement<List<PostgresJoinConfig>>(it) } ?: listOf() //server postgres changes
        channel.callbackManager.setServerChanges(serverPostgresChanges)
        if(channel.status.value != RealtimeChannel.Status.SUBSCRIBED) {
            Realtime.logger.d { "Joined channel ${message.topic}" }
            channel.updateStatus(RealtimeChannel.Status.SUBSCRIBED)
        }
    }

    override fun appliesTo(message: RealtimeMessage): Boolean {
        return message.event == RealtimeChannel.CHANNEL_EVENT_REPLY && message.payload["response"]?.jsonObject?.containsKey(
            RealtimeChannel.CHANNEL_EVENT_POSTGRES_CHANGES
        ) ?: false
    }

}
