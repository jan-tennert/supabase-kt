package io.github.jan.supabase.realtime

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.annotiations.SupabaseInternal
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.plugins.MainConfig
import io.github.jan.supabase.plugins.MainPlugin
import io.github.jan.supabase.plugins.SupabasePluginProvider
import io.github.jan.supabase.supabaseJson
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Plugin for interacting with the supabase realtime api
 *
 * To use it you need to install it to the [SupabaseClient]:
 * ```kotlin
 * val client = createSupabaseClient(supabaseUrl, supabaseKey) {
 *    install(Realtime)
 * }
 * ```
 *
 * then you have to connect to the websocket:
 * ```kotlin
 * client.realtime.connect()
 * ```
 * You can then subscribe to a channel:
 * ```kotlin
 * val channel = client.realtime.createChannel("channelId")
 * ```
 * You can then listen to events on the channel:
 * ```kotlin
 * val productChangeFlow = channel.postgrestChangeFlow<PostgrestAction.Insert>(schema = "public") {
 *    table = "products"
 * }.map { it.decodeRecord<Product>() }
 * ```
 * And at last you have to join the channel:
 * ```kotlin
 * channel.join()
 * ```
 */
sealed interface Realtime : MainPlugin<Realtime.Config> {

    /**
     * The current status of the realtime connection
     */
    val status: StateFlow<Status>

    /**
     * A map of all active the subscriptions
     */
    val subscriptions: Map<String, RealtimeChannel>

    /**
     * Connects to the realtime websocket. The url will be taken from the custom provided [Realtime.Config.customRealtimeURL] or [SupabaseClient]
     */
    suspend fun connect()

    /**
     * Disconnects from the realtime websocket
     */
    fun disconnect()

    @SupabaseInternal
    fun RealtimeChannel.addChannel(channel: RealtimeChannel)

    @SupabaseInternal
    fun RealtimeChannel.removeChannel(topic: String)

    /**
     * Blocks your current coroutine until the websocket connection is closed
     */
    suspend fun block()

    /**
     * @property websocketConfig Custom configuration for the ktor websocket
     * @property secure Whether to use wss or ws. Defaults to [SupabaseClient.useHTTPS] when null
     * @property customRealtimeURL Custom url for the realtime websocket. Uses [SupabaseClient.supabaseUrl] by default
     * @property disconnectOnSessionLoss Whether to disconnect from the websocket when the session is lost. Defaults to true
     * @property reconnectDelay The delay between reconnect attempts. Defaults to 7 seconds
     * @property heartbeatInterval The interval between heartbeat messages. Defaults to 15 seconds
     */
    data class Config(
        var websocketConfig: WebSockets.Config.() -> Unit = {},
        var secure: Boolean? = null,
        var heartbeatInterval: Duration = 15.seconds,
        var customRealtimeURL: String? = null,
        var reconnectDelay: Duration = 7.seconds,
        override var customUrl: String? = null,
        override var jwtToken: String? = null,
        var disconnectOnSessionLoss: Boolean = true
    ): MainConfig

    companion object : SupabasePluginProvider<Config, Realtime> {

        override val key = "realtime"
        const val API_VERSION = 1

        override fun createConfig(init: Config.() -> Unit) = Config().apply(init)
        override fun setup(builder: SupabaseClientBuilder, config: Config) {
            builder.httpConfig {
                install(WebSockets) {
                    contentConverter = KotlinxWebsocketSerializationConverter(supabaseJson)
                    config.websocketConfig(this)
                }
            }
        }

        override fun create(supabaseClient: SupabaseClient, config: Config): Realtime = RealtimeImpl(supabaseClient, config)

    }

    enum class Status {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
    }

}

internal class RealtimeImpl(override val supabaseClient: SupabaseClient, override val config: Realtime.Config) :
    Realtime {

    var ws: DefaultClientWebSocketSession? = null
    private val _status = MutableStateFlow(Realtime.Status.DISCONNECTED)
    override val status: StateFlow<Realtime.Status> = _status.asStateFlow()
    private val _subscriptions = mutableMapOf<String, RealtimeChannel>()
    override val subscriptions: Map<String, RealtimeChannel>
        get() = _subscriptions.toMap()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var heartbeatJob: Job? = null
    var messageJob: Job? = null
    var ref = 0
    var heartbeatRef = 0
    override val API_VERSION: Int
        get() = Realtime.API_VERSION

    override val PLUGIN_KEY: String
        get() = Realtime.key

    init {
        if(config.secure == null) {
            config.secure = supabaseClient.useHTTPS
        }
    }

    override suspend fun connect() = connect(false)

    suspend fun connect(reconnect: Boolean) {
        if (reconnect) {
            delay(config.reconnectDelay)
            Napier.d { "Reconnecting..." }
        } else {
            scope.launch {
                supabaseClient.pluginManager.getPluginOrNull(GoTrue)?.sessionStatus?.collect {
                    when(it) {
                        is SessionStatus.Authenticated -> updateJwt(it.session.accessToken)
                        is SessionStatus.NotAuthenticated -> {
                            if(config.disconnectOnSessionLoss) {
                                Napier.w { "No auth session found, disconnecting from realtime websocket"}
                                disconnect()
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
        if (status.value == Realtime.Status.CONNECTED) throw IllegalStateException("Websocket already connected")
        val prefix = if (config.secure == true) "wss://" else "ws://"
        _status.value = Realtime.Status.CONNECTING
        val realtimeUrl = config.customRealtimeURL ?: (prefix + supabaseClient.supabaseUrl + ("/realtime/v${Realtime.API_VERSION}/websocket?apikey=${supabaseClient.supabaseKey}&vsn=1.0.0"))
        try {
            ws = supabaseClient.httpClient.webSocketSession(realtimeUrl)
            _status.value = Realtime.Status.CONNECTED
            Napier.i { "Connected to realtime websocket!" }
            listenForMessages()
            startHeartbeating()
             if(reconnect) {
                 rejoinChannels()
             }
        } catch(e: Exception) {
             Napier.e(e) { "Error while trying to connect to realtime websocket. Trying again in ${config.reconnectDelay}" }
             disconnect()
             connect(true)
        }
    }

    private fun rejoinChannels() {
        scope.launch {
            for (channel in _subscriptions.values) {
                channel.join()
            }
        }
    }

    private fun listenForMessages() {
        messageJob = scope.launch {
            try {
                ws?.let {
                    for (frame in it.incoming) {
                        val message = frame as? Frame.Text ?: continue
                        onMessage(message.readText())
                    }
                }
            } catch(e: Exception) {
                if(!isActive) return@launch
                Napier.e(e) { "Error while listening for messages. Trying again in ${config.reconnectDelay}" }
                disconnect()
                connect(true)
            }
        }
    }

    private fun startHeartbeating() {
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(config.heartbeatInterval)
                if(!isActive) break
                launch {
                    sendHeartbeat()
                }
            }
        }
    }

    override fun disconnect() {
        Napier.d { "Closing websocket connection" }
        messageJob?.cancel()
        ws?.cancel()
        ws = null
        heartbeatJob?.cancel()
        _status.value = Realtime.Status.DISCONNECTED
    }

    private fun onMessage(stringMessage: String) {
        val message = supabaseJson.decodeFromString<RealtimeMessage>(stringMessage)
        Napier.d { "Received message $stringMessage" }
        val channel = subscriptions[message.topic] as? RealtimeChannelImpl
        if(message.ref?.toIntOrNull() == heartbeatRef) {
            Napier.i { "Heartbeat received" }
            heartbeatRef = 0
        } else {
            Napier.d { "Received event ${message.event} for channel ${channel?.topic}" }
            channel?.onMessage(message)
        }
    }

    private fun updateJwt(jwt: String) {
        scope.launch {
            subscriptions.values.filter { it.status.value == RealtimeChannel.Status.JOINED }.forEach { it.updateAuth(jwt) }
        }
    }

    private suspend fun sendHeartbeat() {
        if (heartbeatRef != 0) {
            heartbeatRef = 0
            ref = 0
            Napier.e { "Heartbeat timeout. Trying to reconnect in ${config.reconnectDelay}" }
            scope.launch {
                disconnect()
                connect(true)
            }
            return
        }
        Napier.d { "Sending heartbeat" }
        heartbeatRef = ++ref
        ws?.sendSerialized(RealtimeMessage("phoenix", "heartbeat", buildJsonObject { }, heartbeatRef.toString()))
    }

    @SupabaseInternal
    override fun RealtimeChannel.removeChannel(topic: String) {
        _subscriptions.remove(topic)
    }

    @SupabaseInternal
    override fun RealtimeChannel.addChannel(channel: RealtimeChannel) {
        _subscriptions[channel.topic] = channel
    }

    override suspend fun close() {
        ws?.cancel()
    }

    override suspend fun block() {
        ws?.coroutineContext?.job?.join() ?: throw IllegalStateException("No connection available")
    }

    override suspend fun parseErrorResponse(response: HttpResponse): RestException {
        return UnknownRestException("Unknown error in realtime plugin", response)
    }

}

/**
 * Creates a new [RealtimeChannel]
 */
inline fun Realtime.createChannel(channelId: String, builder: RealtimeChannelBuilder.() -> Unit = {}): RealtimeChannel {
    return RealtimeChannelBuilder("realtime:$channelId", this as RealtimeImpl).apply(builder).build()
}

/**
 * Supabase Realtime is a way to listen to changes in the PostgreSQL database via websockets
 */
val SupabaseClient.realtime: Realtime
    get() = pluginManager.getPlugin(Realtime)