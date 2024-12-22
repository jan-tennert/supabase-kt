import com.apollographql.apollo.api.http.HttpHeader
import io.github.jan.supabase.BuildConfig
import io.github.jan.supabase.graphql.ApolloHttpInterceptor
import io.github.jan.supabase.graphql.GraphQL
import io.github.jan.supabase.graphql.graphql
import io.github.jan.supabase.testing.TEST_JWT
import io.github.jan.supabase.testing.createMockedSupabaseClient
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GraphQLTest {

    @Test
    fun testCreatedGraphQLClient() {
        val supabaseClient = createMockedSupabaseClient(
            supabaseUrl = "https://test.supabase.co",
            supabaseKey = TEST_JWT,
            configuration = {
                install(GraphQL) {
                    apolloConfiguration {
                        webSocketIdleTimeoutMillis(1000)
                    }
                }
            }
        )
        val client = supabaseClient.graphql.apolloClient
        assertEquals("https://test.supabase.co/graphql/v1", client.newBuilder().httpServerUrl)
        assertNotNull(client.httpHeaders)
        assertContains(client.httpHeaders!!, HttpHeader("apikey", TEST_JWT))
        assertContains(client.httpHeaders!!, HttpHeader("X-Client-Info", "supabase-kt/${BuildConfig.PROJECT_VERSION}"))
        assertTrue { client.newBuilder().httpInterceptors.any { it is ApolloHttpInterceptor } }
        assertEquals(1000, client.newBuilder().webSocketIdleTimeoutMillis)
    }

}