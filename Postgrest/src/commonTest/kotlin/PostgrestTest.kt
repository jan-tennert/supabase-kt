import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.RpcMethod
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.request.InsertRequestBuilder
import io.github.jan.supabase.postgrest.query.request.UpsertRequestBuilder
import io.github.jan.supabase.postgrest.query.select
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.github.jan.supabase.testing.assertMethodIs
import io.github.jan.supabase.testing.assertPathIs
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostgrestTest {

    private val configureClient: SupabaseClientBuilder.() -> Unit = {
        install(Postgrest)
    }

    @Test
    fun testSelectHttpMethodGet() {
        val columns = Columns.list("column1", "column2")
        testClient(
            request = { table ->
                from(table).select(columns) {
                    headers["custom"] = "value"
                    params["custom"] = listOf("value")
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Get, it.method)
                assertEquals(columns.value, it.url.parameters["select"])
                assertEquals("value", it.headers["custom"])
                assertEquals("value", it.url.parameters["custom"])
                respond("")
            }
        )
    }

    @Test
    fun testSelectWithSelectableType() {
        testClient(
            postgrestConfig = {
                columnRegistry.registerColumns("TestType", "column1,column2")
            },
            request = { table ->
                from(table).select<TestType> {
                    headers["custom"] = "value"
                    params["custom"] = listOf("value")
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Get, it.method)
                assertEquals("column1,column2", it.url.parameters["select"])
                assertEquals("value", it.headers["custom"])
                assertEquals("value", it.url.parameters["custom"])
                respond("")
            }
        )
    }

    @Test
    fun testSelectSchema() {
        val columns = Columns.list("column1", "column2")
        testClient(
            request = { table ->
                from("schema", table).select(columns)
            },
            requestHandler = {
                assertEquals("schema", it.headers["Accept-Profile"])
                assertMethodIs(HttpMethod.Get, it.method)
                assertEquals(columns.value, it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testSelectHttpMethodHead() {
        val columns = Columns.list("column1", "column2")
        testClient(
            request = { table ->
                from(table).select(columns) {
                    head = true
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Head, it.method)
                assertEquals(columns.value, it.url.parameters["select"])
                respond("")
            }
        )
    }

    @Test
    fun testInsert() {
        insertTestClient(
            request = {
            },
            requestHandler = {
                val prefer = it.headers["Prefer"]?.split(",") ?: emptyList()
                assertContains(prefer, "return=minimal") //default
            }
        )
    }

    @Test
    fun testInsertDefaultToNull() {
        insertTestClient(
            request = {
                defaultToNull = false
            },
            requestHandler = {
                val prefer = it.headers["Prefer"]?.split(",") ?: emptyList()
                assertContains(prefer, "return=minimal") //default
                assertContains(prefer, "missing=default")
            }
        )
    }

    @Test
    fun testInsertWithSelect() {
        insertTestClient(
            request = {
                select(Columns.raw("column1,column2"))
            },
            requestHandler = {
                val prefer = it.headers["Prefer"]?.split(",") ?: emptyList()
                assertEquals("column1,column2", it.url.parameters["select"])
                assertContains(prefer, "return=representation")
            }
        )
    }

    @Test
    fun testInsertEmptyObject() {
        insertTestClient(
            mockData = buildJsonObject {  },
            request = {
            },
            requestHandler = {
            }
        )
    }

    private fun insertTestClient(
        mockData: JsonObject = buildJsonObject {
            put("column1", "value1")
            put("column2", "value2")
        },
        request: InsertRequestBuilder.() -> Unit,
        requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> Unit,
    ) {
        testClient(
            request = { table ->
                from("schema", table).insert(mockData) {
                    request()
                    headers["custom"] = "value"
                    params["custom"] = listOf("value")
                }
            },
            requestHandler = {
                assertEquals(mockData, it.body.toJsonElement().jsonArray.first())
                requestHandler(it)
                assertMethodIs(HttpMethod.Post, it.method)
                if(mockData.isEmpty()) {
                    assertNull(it.url.parameters["columns"])
                } else {
                    assertEquals("column1,column2", it.url.parameters["columns"])
                }
                assertEquals("value", it.headers["custom"])
                assertEquals("value", it.url.parameters["custom"])
                assertEquals("schema", it.headers["Content-Profile"])
                respond("")
            }
        )
    }

    @Test
    fun testUpsert() {
        upsertTestClient(
            request = {
            },
            requestHandler = {
                val prefer = it.headers["Prefer"]?.split(",") ?: emptyList()
                assertContains(prefer, "resolution=merge-duplicates") //default
            }
        )
    }

    @Test
    fun testUpsertIgnoreDuplicates() {
        upsertTestClient(
            request = {
                ignoreDuplicates = true
            },
            requestHandler = {
                val prefer = it.headers["Prefer"]?.split(",") ?: emptyList()
                assertContains(prefer, "resolution=ignore-duplicates")
            }
        )
    }

    @Test
    fun testUpsertWithSelect() {
        upsertTestClient(
            request = {
                select(Columns.raw("column1,column2"))
            },
            requestHandler = {
                val prefer = it.headers["Prefer"]?.split(",") ?: emptyList()
                assertEquals("column1,column2", it.url.parameters["select"])
                assertContains(prefer, "return=representation")
            }
        )
    }

    @Test
    fun testUpsertWithDefaultToNull() {
        upsertTestClient(
            request = {
                defaultToNull = false
            },
            requestHandler = {
                val prefer = it.headers["Prefer"]?.split(",") ?: emptyList()
                assertContains(prefer, "missing=default")
            }
        )
    }

    @Test
    fun testUpsertEmptyObject() {
        upsertTestClient(
            mockData = buildJsonArray {  },
            request = {
            },
            requestHandler = {
            }
        )
    }

    @Test
    fun testUpsertWithOnConflict() {
        upsertTestClient(
            request = {
                onConflict = "column1"
            },
            requestHandler = {
                assertEquals("column1", it.url.parameters["on_conflict"])
            }
        )
    }

    private fun upsertTestClient(
        mockData: JsonArray = buildJsonArray {
            add(buildJsonObject {
                put("column1", "value1")
                put("column2", "value2")
            })
            add(buildJsonObject {
                put("column1", "value1")
                put("column3", "value3")
            })
        },
        request: UpsertRequestBuilder.() -> Unit,
        requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> Unit,
    ) {

        testClient(
            request = { table ->
                from("schema", table).upsert(mockData) {
                    request()
                    headers["custom"] = "value"
                    params["custom"] = listOf("value")
                }
            },
            requestHandler = {
                assertEquals(mockData, it.body.toJsonElement())
                requestHandler(it)
                assertMethodIs(HttpMethod.Post, it.method)
                if(mockData.isEmpty() || mockData.all { it.jsonObject.isEmpty() }) {
                    assertNull(it.url.parameters["columns"])
                } else {
                    assertEquals("column1,column2,column3", it.url.parameters["columns"])
                }
                assertEquals("value", it.headers["custom"])
                assertEquals("schema", it.headers["Content-Profile"])
                assertEquals("value", it.url.parameters["custom"])
                respond("")
            }
        )
    }

    @Test
    fun testUpdate() {
        val columns = Columns.list("column1", "column2")
        val mockData = buildJsonObject {
            put("column1", "value1")
            put("column2", "value2")
        }
        testClient(
            request = { table ->
                from(table).update(mockData) {
                    select(columns)
                    headers["custom"] = "value"
                    params["custom"] = listOf("value")
                }
            },
            requestHandler = {
                assertMethodIs(HttpMethod.Patch, it.method)
                assertEquals(mockData, it.body.toJsonElement())
                val prefer = it.headers["Prefer"]?.split(",") ?: emptyList()
                assertEquals("column1,column2", it.url.parameters["select"])
                assertContains(prefer, "return=representation")
                assertEquals("value", it.headers["custom"])
                assertEquals("value", it.url.parameters["custom"])
                respond("")
            }
        )
    }

    @Test
    fun testRpcNoParameters() {
        val supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            assertPathIs("/rpc/function", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Head, it.method)
            assertEquals("schema", it.headers["Accept-Profile"])
            assertEquals("value", it.headers["custom"])
            assertEquals("value", it.url.parameters["custom"])
            respond("")
        }
        runTest {
            supabase.postgrest.rpc("function") {
                method = RpcMethod.HEAD
                schema = "schema"
                headers["custom"] = "value"
                params["custom"] = listOf("value")
            }
        }
    }

    @Test
    fun testRpcParameters() {
        val mockData = buildJsonObject {
            put("key", "value")
        }
        val supabase = createMockedSupabaseClient(
            configuration = configureClient
        ) {
            assertPathIs("/rpc/function", it.url.pathAfterVersion())
            assertEquals(mockData, it.body.toJsonElement())
            assertMethodIs(HttpMethod.Post, it.method)
            assertEquals("schema", it.headers["Content-Profile"])
            respond("")
        }
        runTest {
            supabase.postgrest.rpc("function", mockData) {
                schema = "schema"
            }
        }
    }

    private fun testClient(
        table: String = "table",
        postgrestConfig: Postgrest.Config.() -> Unit = {},
        request: suspend SupabaseClient.(table: String) -> PostgrestResult,
        requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { respond("")},
    ) {
        val supabase = createMockedSupabaseClient(
            configuration = {
                install(Postgrest) {
                    postgrestConfig()
                }
            }
        ) {
            assertPathIs("/$table", it.url.pathAfterVersion())
            requestHandler(it)
        }
        runTest {
            supabase.request(table)
        }
    }

}