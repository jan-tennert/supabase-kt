import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.api.http.get
import com.apollographql.apollo.network.http.HttpInterceptorChain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.graphql.ApolloHttpInterceptor
import io.github.jan.supabase.graphql.GraphQL
import io.github.jan.supabase.graphql.graphql
import io.github.jan.supabase.testing.TEST_JWT
import io.github.jan.supabase.testing.createMockedSupabaseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApolloHttpInterceptorTest {

    @Test
    fun testApolloHttpInterceptorWithApiKey() {
        runTest {
            val supabaseClient = createMockedSupabaseClient(
                supabaseKey = TEST_JWT,
                configuration = {
                    install(GraphQL)
                }
            )
            testInterceptor(supabaseClient, TEST_JWT)
        }
    }

    @Test
    fun testApolloHttpInterceptorWithAuthToken() {
        runTest {
            val supabaseClient = createMockedSupabaseClient(
                configuration = {
                    install(GraphQL)
                    install(Auth) {
                        minimalSettings()
                    }
                }
            )
            supabaseClient.auth.importAuthToken(TEST_JWT)
            testInterceptor(supabaseClient, TEST_JWT)
        }
    }

    @Test
    fun testApolloHttpInterceptorWithAccessToken() {
        runTest {
            val supabaseClient = createMockedSupabaseClient(
                configuration = {
                    install(GraphQL)
                    accessToken = {
                        TEST_JWT
                    }
                }
            )
            testInterceptor(supabaseClient, TEST_JWT)
        }
    }

    @Test
    fun testApolloHttpInterceptorWithACustomToken() {
        runTest {
            val supabaseClient = createMockedSupabaseClient(
                configuration = {
                    install(GraphQL) {
                        jwtToken = TEST_JWT
                    }
                }
            )
            testInterceptor(supabaseClient, TEST_JWT)
        }
    }

    @OptIn(ApolloExperimental::class)
    private suspend fun testInterceptor(supabaseClient: SupabaseClient, token: String) {
        val interceptor = ApolloHttpInterceptor(supabaseClient, supabaseClient.graphql.config)
        interceptor.intercept(HttpRequest.Builder(HttpMethod.Get, "").build(), object : HttpInterceptorChain {

            override suspend fun proceed(request: HttpRequest): HttpResponse {
                assertEquals("Bearer $token", request.headers.get("Authorization"))
                return HttpResponse.Builder(200).build()
            }

        })
    }

}
