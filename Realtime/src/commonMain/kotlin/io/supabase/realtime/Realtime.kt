package io.supabase.realtime

import io.supabase.SupabaseClient
import io.supabase.SupabaseClientBuilder
import io.supabase.SupabaseSerializer
import io.supabase.annotations.SupabaseInternal
import io.supabase.logging.SupabaseLogger
import io.supabase.plugins.CustomSerializationConfig
import io.supabase.plugins.CustomSerializationPlugin
import io.supabase.plugins.MainConfig
import io.supabase.plugins.MainPlugin
import io.supabase.plugins.SupabasePluginProvider
import io.supabase.realtime.websocket.RealtimeWebsocketFactory
import io.supabase.serializer.KotlinXSerializer
import io.supabase.supabaseJson
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Plugin for interacting with the supabase realtime api
 *
 * To use it you need to install it to the [SupabaseClient]:
 * ```kotlin
 * val supabase = createSupabaseClient(supabaseUrl, supabaseKey) {
 *    install(Realtime)
 * }
 * ```
 *
 * You can then create a channel:
 * ```kotlin
 * val channel = supabase.realtime.channel("channelId")
 * ```
 * Then listen to events on the channel:
 * ```kotlin
 * val productChangeFlow = channel.postgrestChangeFlow<PostgrestAction.Insert>(schema = "public") {
 *    table = "products"
 * }.map { it.decodeRecord<Product>() }
 * ```
 * And at last you have to subscribe to the channel:
 * ```kotlin
 * channel.subscribe()
 * ```
 */
sealed interface Realtime : MainPlugin<Realtime.Config>, CustomSerializationPlugin {

    /**
     * The current status of the realtime connection
     */
    val status: StateFlow<Status>

    /**
     * A map of all active the subscriptions
     */
    val subscriptions: Map<String, RealtimeChannel>

    /**
     * Connects to the realtime websocket. The url will be taken from the custom provided [Realtime.Config.customUrl] or [SupabaseClient]
     */
    suspend fun connect()

    /**
     * Disconnects from the realtime websocket
     */
    fun disconnect()

    @SupabaseInternal
    fun Realtime.addChannel(channel: RealtimeChannel)

    /**
     * Unsubscribes and removes a channel from the [subscriptions]
     * @param channel The channel to remove
     */
    suspend fun removeChannel(channel: RealtimeChannel)

    /**
     * Unsubscribes and removes all channels from the [subscriptions]
     */
    suspend fun removeAllChannels()

    /**
     * Blocks your current coroutine until the websocket connection is closed
     */
    suspend fun block()

    /**
     * Sends a message to the realtime websocket
     * @param message The message to send
     */
    @SupabaseInternal
    suspend fun send(message: RealtimeMessage)

    /**
     * @property websocketConfig Custom configuration for the Ktor Websocket Client. This only applies if [Realtime.Config.websocketFactory] is null.
     * @property secure Whether to use wss or ws. Defaults to [SupabaseClient.useHTTPS] when null
     * @property disconnectOnSessionLoss Whether to disconnect from the websocket when the session is lost. Defaults to true
     * @property reconnectDelay The delay between reconnect attempts. Defaults to 7 seconds
     * @property heartbeatInterval The interval between heartbeat messages. Defaults to 15 seconds
     * @property connectOnSubscribe Whether to connect to the websocket when subscribing to a channel. Defaults to true
     * @property disconnectOnNoSubscriptions Whether to disconnect from the websocket when there are no more subscriptions. Defaults to true
     * @property serializer A serializer used for serializing/deserializing objects e.g. in [PresenceAction.decodeJoinsAs] or [RealtimeChannel.broadcast]. Defaults to [KotlinXSerializer]
     * @property websocketFactory A custom websocket factory. If this is set, the [websocketConfig] will be ignored
     */
    data class Config(
        var websocketConfig: WebSockets.Config.() -> Unit = {},
        var secure: Boolean? = null,
        var heartbeatInterval: Duration = 15.seconds,
        var reconnectDelay: Duration = 7.seconds,
        var disconnectOnSessionLoss: Boolean = true,
        var connectOnSubscribe: Boolean = true,
        @property:SupabaseInternal var websocketFactory: RealtimeWebsocketFactory? = null,
        var disconnectOnNoSubscriptions: Boolean = true,
    ): MainConfig(), CustomSerializationConfig {

        override var serializer: SupabaseSerializer? = null

    }

    companion object : SupabasePluginProvider<Config, Realtime> {

        override val key = "realtime"

        override val logger: SupabaseLogger = SupabaseClient.createLogger("Supabase-Realtime")

        /**
         * The current realtime api version
         */
        const val API_VERSION = 1

        override fun createConfig(init: Config.() -> Unit) = Config().apply(init)
        override fun setup(builder: SupabaseClientBuilder, config: Config) {
            if(config.websocketFactory == null) {
                builder.httpConfig {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(supabaseJson)
                        config.websocketConfig(this)
                    }
                }
            }
        }

        override fun create(supabaseClient: SupabaseClient, config: Config): Realtime = RealtimeImpl(supabaseClient, config)

    }

    /**
     * The current status of the realtime connection
     */
    enum class Status {
        /**
         * [Realtime] is disconnected from Supabase Realtime
         */
        DISCONNECTED,

        /**
         * [Realtime] is connecting to Supabase Realtime
         */
        CONNECTING,

        /**
         * [Realtime] is connected to Supabase Realtime
         */
        CONNECTED,
    }

}

/**
 * Creates a new [RealtimeChannel]
 */
inline fun Realtime.channel(channelId: String, builder: RealtimeChannelBuilder.() -> Unit = {}): RealtimeChannel {
    return RealtimeChannelBuilder("realtime:$channelId", this as RealtimeImpl).apply(builder).build()
}

/**
 * Supabase Realtime is a way to listen to changes in the PostgreSQL database via websockets
 */
val SupabaseClient.realtime: Realtime
    get() = pluginManager.getPlugin(Realtime)

/**
 * Creates a new [RealtimeChannel]
 */
inline fun SupabaseClient.channel(channelId: String, builder: RealtimeChannelBuilder.() -> Unit = {}): RealtimeChannel = realtime.channel(channelId, builder)