package io.supabase.realtime.event

import io.supabase.logging.d
import io.supabase.realtime.Realtime
import io.supabase.realtime.RealtimeChannel
import io.supabase.realtime.RealtimeMessage

/**
 * Event that handles the closing of a channel
 */
data object RCloseEvent : RealtimeEvent {

    override suspend fun handle(channel: RealtimeChannel, message: RealtimeMessage) {
        channel.realtime.removeChannel(channel)
        Realtime.Companion.logger.d { "Unsubscribed from channel ${message.topic}" }
    }

    override fun appliesTo(message: RealtimeMessage): Boolean {
        return message.event == RealtimeChannel.Companion.CHANNEL_EVENT_CLOSE
    }

}