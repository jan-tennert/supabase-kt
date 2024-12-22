import app.cash.turbine.test
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.CallbackManagerImpl
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.PostgresJoinConfig
import io.github.jan.supabase.realtime.Presence
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.RealtimeChannel.Companion.CHANNEL_EVENT_REPLY
import io.github.jan.supabase.realtime.RealtimeChannel.Companion.CHANNEL_EVENT_SYSTEM
import io.github.jan.supabase.realtime.RealtimeJoinPayload
import io.github.jan.supabase.realtime.RealtimeMessage
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.testing.TEST_JWT
import io.github.jan.supabase.testing.assertPathIs
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RealtimeChannelTest {

    @Test
    fun testConnectOnSubscribeDisabled() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                    //Does not matter for this test
                },
                supabaseHandler = {
                    val channel = it.channel("")
                    assertFailsWith<IllegalStateException>() {
                        channel.subscribe()
                    }
                },
                realtimeConfig = {
                    connectOnSubscribe = false
                }
            )
        }
    }

    @Test
    fun testConnectOnSubscribeEnabled() {
        runTest {
            createTestClient(
                wsHandler = { i, _ ->
                    i.receive()
                },
                supabaseHandler = {
                    val channel = it.channel("")
                    channel.subscribe(false)
                    assertEquals(Realtime.Status.CONNECTED, it.realtime.status.value)
                }
            )
        }
    }

    @Test
    fun testChannelStatusWithoutPostgres() {
        val channelId = "channelId"
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    i.receive()
                    o.send(RealtimeMessage("realtime:$channelId", CHANNEL_EVENT_SYSTEM, buildJsonObject { put("status", "ok") }, ""))
                    i.receive()
                    o.send(RealtimeMessage("realtime:$channelId", CHANNEL_EVENT_REPLY, buildJsonObject { put("status", "ok") }, ""))
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    assertEquals(channel.status.value, RealtimeChannel.Status.UNSUBSCRIBED)
                    assertEquals(it.realtime.status.value, Realtime.Status.DISCONNECTED)
                    channel.subscribe(blockUntilSubscribed = true)
                    assertEquals(channel.status.value, RealtimeChannel.Status.SUBSCRIBED)
                    channel.unsubscribe()
                    assertEquals(channel.status.value, RealtimeChannel.Status.UNSUBSCRIBING)
                    assertEquals(RealtimeChannel.Status.UNSUBSCRIBED, channel.status.waitForValue(RealtimeChannel.Status.UNSUBSCRIBED))
                },
            )
        }
    }

    @Test
    fun testSendingPayloadWithoutJWT() {
        val expectedChannelId = "channelId"
        val expectedIsPrivate = true
        val expectedReceiveOwnBroadcasts = true
        val expectedAcknowledge = true
        val expectedPresenceKey = "presenceKey"
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    val message = i.receive()
                    val payload = Json.decodeFromJsonElement<RealtimeJoinPayload>(message.payload)
                    assertEquals("realtime:$expectedChannelId", message.topic)
                    assertEquals(expectedIsPrivate, payload.config.isPrivate)
                    assertEquals(expectedReceiveOwnBroadcasts, payload.config.broadcast.receiveOwnBroadcasts)
                    assertEquals(expectedAcknowledge, payload.config.broadcast.acknowledgeBroadcasts)
                    assertEquals(expectedPresenceKey, payload.config.presence.key)
                },
                supabaseHandler = {
                    val channel = it.channel("channelId") {
                        isPrivate = expectedIsPrivate
                        broadcast {
                            receiveOwnBroadcasts = expectedReceiveOwnBroadcasts
                            acknowledgeBroadcasts = expectedAcknowledge
                        }
                        presence {
                            key = expectedPresenceKey
                        }
                    }
                    channel.subscribe()
                }
            )
        }
    }

    @Test
    fun testSendingPayloadWithAuthJWT() {
        val expectedAuthToken = TEST_JWT
        runTest {
            createTestClient(
                wsHandler = { i, _ ->
                    val message = i.receive()
                    assertEquals(expectedAuthToken, message.payload["access_token"]?.jsonPrimitive?.content)
                },
                supabaseHandler = {
                    it.auth.importAuthTokenValid(expectedAuthToken)
                    val channel = it.channel("channelId")
                    channel.subscribe()
                },
                supabaseConfig = {
                    install(Auth) {
                        minimalSettings()
                    }
                }
            )
        }
    }

    @Test
    fun testSendingPayloadWithCustomJWT() {
        val expectedAuthToken = "authToken"
        runTest {
            createTestClient(
                wsHandler = { i, _ ->
                    val message = i.receive()
                    assertEquals(expectedAuthToken, message.payload["access_token"]?.jsonPrimitive?.content)
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    channel.subscribe()
                },
                realtimeConfig = {
                    jwtToken = expectedAuthToken
                }
            )
        }
    }

    @Test
    fun testSendingBroadcasts() {
        val message = buildJsonObject {
            put("key", "value")
        }
        val event = "event"
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    handleSubscribe(i, o,"channelId")
                    val rMessage = i.receive()
                    assertEquals("realtime:channelId", rMessage.topic)
                    assertEquals("broadcast", rMessage.event)
                    assertEquals(message, rMessage.payload["payload"]?.jsonObject)
                    assertEquals(event, rMessage.payload["event"]?.jsonPrimitive?.content)
                    assertEquals("broadcast", rMessage.payload["type"]?.jsonPrimitive?.content)
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    channel.subscribe(true)
                    channel.broadcast(event, message)
                }
            )
        }
    }

    @Test
    fun testSendingBroadcastsUnsubscribed() {
        runTest {
            val expectedEvent = "event"
            val expectedMessage = buildJsonObject {
                put("key", "value")
            }
            val expectedChannelId = "channelId"
            val isPrivate = true
            createTestClient(
                wsHandler = { _, _ ->
                },
                supabaseHandler = {
                    val channel = it.channel(expectedChannelId) {
                        this.isPrivate = isPrivate
                    }
                    channel.broadcast(expectedEvent, expectedMessage)
                },
                mockEngineHandler = {
                    assertPathIs("/api/broadcast", it.url.pathAfterVersion())
                    val body = it.body.toJsonElement().jsonObject["messages"]?.jsonArray?.firstOrNull()?.jsonObject ?: error("No messages in body")
                    assertEquals(expectedEvent, body["event"]?.jsonPrimitive?.content)
                    assertEquals(expectedMessage, body["payload"]?.jsonObject)
                    assertEquals(isPrivate, body["private"]?.jsonPrimitive?.boolean)
                    assertEquals(expectedChannelId, body["topic"]?.jsonPrimitive?.content)
                    respond("")
                }
            )
        }
    }

    @Test
    fun testSendingPresenceUnsubscribed() {
        runTest {
            createTestClient(
                wsHandler = { _, _ ->
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    assertFailsWith<IllegalStateException> {
                        channel.track(buildJsonObject {  })
                    }
                }
            )
        }
    }

    @Test
    fun testReceivingBroadcasts() {
        val event = "event"
        val channelId = "channelId"
        val amount = 10
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    handleSubscribe(i, o, channelId)
                    for(i in 0..amount) {
                        o.sendBroadcast(channelId, event, buildJsonObject { put("key", i) })
                    }
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    coroutineScope {
                        launch {
                            val broadcastFlow = channel.broadcastFlow<JsonObject>(event)
                            broadcastFlow.test(FLOW_TIMEOUT) {
                                for(i in 0..amount) {
                                    assertEquals(i, awaitItem()["key"]?.jsonPrimitive?.int)
                                }
                            }
                        }
                        launch {
                            channel.subscribe(true)
                        }
                    }.join()
                }
            )
        }
    }

    @Test
    fun testReceivingPresence() {
        val channelId = "channelId"
        val amount = 10
        runTest {
            createTestClient(
                wsHandler = { i, o ->
                    handleSubscribe(i, o, channelId)
                    for(i in 0..amount) {
                        o.sendPresence(
                            channelId,
                            mapOf("userId" to Presence(i.toString(), buildJsonObject { put("key", i) })),
                            mapOf("userId" to Presence((i-1).toString(), buildJsonObject { put("key", i-1) })
                        ))
                    }
                },
                supabaseHandler = {
                    val channel = it.channel("channelId")
                    coroutineScope {
                        launch {
                            val presenceFlow = channel.presenceChangeFlow()
                            presenceFlow.test(FLOW_TIMEOUT) {
                                for(index in 0..amount) {
                                    val value = awaitItem()
                                    val joins =  value.joins
                                    val leaves = value.leaves
                                    assertEquals(1, joins.size)
                                    assertEquals(1, leaves.size)
                                    assertEquals("userId", joins.keys.first())
                                    assertEquals("userId", leaves.keys.first())
                                    assertEquals(index.toString(), joins.values.first().presenceRef)
                                    assertEquals((index-1).toString(), leaves.values.first().presenceRef)
                                    assertEquals(index, joins.values.first().state["key"]?.jsonPrimitive?.int)
                                    assertEquals(index-1, leaves.values.first().state["key"]?.jsonPrimitive?.int)
                                }
                            }
                        }
                        launch {
                            channel.subscribe(true)
                        }
                    }.join()
                }
            )
        }
    }

    @Test
    fun testSubscribingWithPostgresChanges() {
        val channelId = "channelId"
        val expectedTable = "table"
        val expectedSchema = "public"
        val expectedFilter = "id=eq.1"
        val events = listOf("INSERT", "UPDATE", "DELETE", "*")
        for(event in events) { //Test if all events are correctly handled using the type parameter
            val postgresServerChanges = listOf(PostgresJoinConfig(expectedSchema, expectedTable, expectedFilter, event, 0L))
            runTest {
                createTestClient(
                    wsHandler = { i, o ->
                        val message = i.receive()
                        assertEquals("realtime:$channelId", message.topic)
                        assertEquals(RealtimeChannel.CHANNEL_EVENT_JOIN, message.event)
                        val payload = Json.decodeFromJsonElement<RealtimeJoinPayload>(message.payload)
                        val postgresChanges = payload.config.postgresChanges
                        assertEquals(1, postgresChanges.size)
                        assertEquals(expectedTable, postgresChanges.first().table)
                        assertEquals(expectedSchema, postgresChanges.first().schema)
                        assertEquals(expectedFilter, postgresChanges.first().filter)
                        assertEquals(event, postgresChanges.first().event)
                        o.send(RealtimeMessage("realtime:$channelId", RealtimeChannel.CHANNEL_EVENT_REPLY, buildJsonObject {
                            put("response", buildJsonObject {
                                put("postgres_changes", Json.encodeToJsonElement(postgresServerChanges))
                            })
                        }, ""))
                    },
                    supabaseHandler = {
                        val channel = it.channel(channelId)
                        val postgresFlow = channel.flowFromEventType(event, expectedSchema, expectedTable, FilterOperation("id", FilterOperator.EQ, 1))
                        channel.subscribe(true)
                        assertEquals(RealtimeChannel.Status.SUBSCRIBED, channel.status.value)
                        assertContentEquals(postgresServerChanges, (channel.callbackManager as CallbackManagerImpl).serverChanges)
                    }
                )
            }
        }
    }

    //For more complex tests we need integration tests

    private fun RealtimeChannel.flowFromEventType(event: String, schema: String, table: String, filter: FilterOperation): Flow<PostgresAction> {
        when(event) {
            "INSERT" -> return postgresChangeFlow<PostgresAction.Insert>(schema) {
                this.table = table
                filter(filter)
            }
            "UPDATE" -> return postgresChangeFlow<PostgresAction.Update>(schema) {
                this.table = table
                filter(filter)
            }
            "DELETE" -> return postgresChangeFlow<PostgresAction.Delete>(schema) {
                this.table = table
                filter(filter)
            }
            "*" -> return postgresChangeFlow<PostgresAction>(schema) {
                this.table = table
                filter(filter)
            }
            else -> error("Unknown event type $event")
        }
    }

}