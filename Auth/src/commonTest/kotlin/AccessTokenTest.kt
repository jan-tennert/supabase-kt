import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.auth.resolveAccessToken
import io.github.jan.supabase.testing.TEST_JWT
import io.github.jan.supabase.testing.createMockedSupabaseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccessTokenTest {

    @Test
    fun testAccessTokenWithJwtToken() {
        runTest {
            val client = createMockedSupabaseClient(
                configuration = {
                    install(Auth) {
                        minimalSettings()
                    }
                }
            )
            client.auth.importAuthToken(TEST_JWT) //this should be ignored as per plugin tokens override the used access token
            assertEquals("myJwtToken", client.resolveAccessToken("myJwtToken"))
        }
    }

    @Test
    fun testAccessTokenWithKeyAsFallback() {
        runTest {
            val client = createMockedSupabaseClient(supabaseKey = TEST_JWT)
            assertEquals(TEST_JWT, client.resolveAccessToken())
        }
    }

    @Test
    fun testAccessTokenWithKeyAsFallbackWithInvalidKey() {
        runTest {
            val client = createMockedSupabaseClient(supabaseKey = "not_a_jwt")
            assertNull(client.resolveAccessToken())
        }
    }

    @Test
    fun testAccessTokenWithoutKey() {
        runTest {
            val client = createMockedSupabaseClient()
            assertNull(client.resolveAccessToken(keyAsFallback = false))
        }
    }

    @Test
    fun testAccessTokenWithCustomAccessToken() {
        runTest {
            val client = createMockedSupabaseClient(
                configuration = {
                    accessToken = {
                        "myCustomToken"
                    }
                }
            )
            assertEquals("myCustomToken", client.resolveAccessToken())
        }
    }

    @Test
    fun testAccessTokenWithAuth() {
        runTest {
            val client = createMockedSupabaseClient(
                configuration = {
                    install(Auth) {
                        minimalSettings()
                    }
                }
            )
            client.auth.importAuthToken(TEST_JWT)
            assertEquals(TEST_JWT, client.resolveAccessToken())
        }
    }

}