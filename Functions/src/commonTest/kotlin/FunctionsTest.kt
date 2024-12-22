import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.functions.FunctionRegion
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.testing.TEST_JWT
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

internal val configuration: SupabaseClientBuilder.() -> Unit = {
    install(Functions)
}

class FunctionsTest {

    @Test
    fun testAuthorizationHeaderAuth() {
        runTest {
            val expectedJWT = TEST_JWT
            val supabase = createMockedSupabaseClient(
                configuration ={
                    install(Auth) {
                        minimalSettings()
                    }
                    configuration()
                },
                requestHandler = {
                    assertEquals("Bearer $expectedJWT", it.headers[HttpHeaders.Authorization])
                    respond("")
                }
            )
            supabase.auth.importAuthToken(expectedJWT)
            supabase.functions.invoke(
                function = ""
            )
        }
    }

    @Test
    fun testAuthorizationHeaderCustomToken() {
        runTest {
            val expectedJWT = TEST_JWT
            val supabase = createMockedSupabaseClient(
                configuration = {
                    install(Functions) {
                        jwtToken = expectedJWT
                    }
                },
                requestHandler = {
                    assertEquals("Bearer $expectedJWT", it.headers[HttpHeaders.Authorization])
                    respond("")
                }
            )
            supabase.functions.invoke(
                function = ""
            )
        }
    }

    @Test
    fun testInvokingWithoutBody() {
        runTest {
            val expectedName = "myFunction"
            val expectedRegion = FunctionRegion.EU_WEST_1
            val expectedHeaders = Headers.build {
                append("myHeader", "myValue")
            }
            val supabase = createMockedSupabaseClient(
                configuration = configuration,
                requestHandler = {
                    assertEquals("POST", it.method.value)
                    assertEquals("/$expectedName", it.url.pathAfterVersion())
                    assertEquals(expectedRegion.value, it.headers["x-region"])
                    assertEquals("myValue", it.headers["myHeader"])
                    respond("")
                }
            )
            supabase.functions.invoke(
                function = expectedName,
                region = expectedRegion,
                headers = expectedHeaders
            )
        }
    }

    @Test
    fun testInvokingWithBody() {
        runTest {
            val expectedName = "myFunction"
            val expectedRegion = FunctionRegion.EU_WEST_1
            val expectedHeaders = Headers.build {
                set(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
            val expectedBody = buildJsonObject {
                put("key", "value")
            }
            val supabase = createMockedSupabaseClient(
                configuration = configuration,
                requestHandler = {
                    val body = it.body.toJsonElement().jsonObject
                    assertEquals(expectedBody, body)
                    assertEquals("POST", it.method.value)
                    assertEquals("/$expectedName", it.url.pathAfterVersion())
                    assertEquals(expectedRegion.value, it.headers["x-region"])
                    respond("")
                }
            )
            supabase.functions.invoke(
                function = expectedName,
                region = expectedRegion,
                body = expectedBody,
                headers = expectedHeaders
            )
        }
    }

}