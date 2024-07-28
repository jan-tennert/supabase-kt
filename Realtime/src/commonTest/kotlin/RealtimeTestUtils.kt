import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.realtime.RealtimeChannel.Companion.CHANNEL_EVENT_SYSTEM
import io.github.jan.supabase.realtime.RealtimeMessage
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.hours

suspend fun Auth.importAuthTokenValid(token: String) {
    importSession(UserSession(token, "", expiresAt = Clock.System.now() + 1.hours, expiresIn = 1.hours.inWholeSeconds, tokenType = ""))
}

suspend fun handleSubscribe(incoming: ReceiveChannel<RealtimeMessage>, outgoing: SendChannel<RealtimeMessage>, channelId: String) {
    incoming.receive()
    outgoing.send(RealtimeMessage("realtime:$channelId", CHANNEL_EVENT_SYSTEM, buildJsonObject { put("status", "ok") }, ""))
}

suspend inline fun <reified T> Flow<T>.waitForValue(value: T) = filter { it == value }.first()