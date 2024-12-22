import io.github.jan.supabase.auth.AuthenticatedSupabaseApi
import io.github.jan.supabase.testing.createMockedSupabaseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AuthenticatedSupabaseApiTest {

    @Test
    fun testFailureIfInvalidAuthorizationHeader() {
        val supabase = createMockedSupabaseClient(
            supabaseKey = "myKey",
            supabaseUrl = "https://example.com"
        )
        val api = AuthenticatedSupabaseApi(
            resolveUrl = { "https://example.com/$it" },
            supabaseClient =  supabase,
            jwtToken = "myToken"
        )
        assertFailsWith<IllegalStateException> {
            runTest {
                api.get("test")
            }
        }
    }

}