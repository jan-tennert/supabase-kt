import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.auth.providers.Github
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.Identity
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.testing.TEST_JWT
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.respondJson
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class AuthTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalSettings()
        }
    }

    @Test
    fun testLoadingSessionFromStorage() {
        runTest {
            val sessionManager = MemorySessionManager(userSession())
            val client = createMockedSupabaseClient(
                configuration = {
                    install(Auth) {
                        minimalSettings(
                            sessionManager = sessionManager,
                            autoLoadFromStorage = true
                        )
                    }
                }
            )
            client.auth.awaitInitialization()
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
        }
    }

    @Test
    fun testErrorWhenUsingAccessToken() {
        runTest {
            assertFailsWith<IllegalStateException> {
                createMockedSupabaseClient(
                    configuration = {
                        accessToken = {
                            "myToken"
                        }
                        install(Auth)
                    }
                )
            }
        }
    }

    @Test
    fun testSavingSessionToStorage() {
        runTest {
            val sessionManager = MemorySessionManager()
            val client = createMockedSupabaseClient(
                configuration = {
                    install(Auth) {
                        minimalSettings(
                            sessionManager = sessionManager,
                            autoSaveToStorage = true
                        )
                    }
                }
            )
            client.auth.awaitInitialization()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
            assertNull(sessionManager.loadSession())
            val session = userSession()
            client.auth.importSession(session)
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
            assertEquals(session, sessionManager.loadSession())
        }
    }

    @Test
    fun testImportExpiredSession() {
        runTest {
            val newSession = userSession()
            val client = createMockedSupabaseClient(configuration = {
                install(Auth) {
                    minimalSettings(
                        alwaysAutoRefresh = true
                    )
                }
            }) {
                respondJson(newSession)
            }
            client.auth.awaitInitialization()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
            val session = userSession(expiresIn = 0)
            client.auth.importSession(session)
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
            assertEquals(newSession.expiresIn, client.auth.currentSessionOrNull()?.expiresIn)
        }
    }

    @Test
    fun testAutoRefreshSession() {
        runTest {
            val newSession = userSession()
            val client = createMockedSupabaseClient(configuration = {
                install(Auth) {
                    minimalSettings(
                        autoLoadFromStorage = false,
                        alwaysAutoRefresh = false,
                        autoSaveToStorage = false
                    )
                }
            }) {
                respondJson(newSession)
            }
            client.auth.awaitInitialization()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
            val session = userSession(expiresIn = 0)
            client.auth.importSession(session)
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
            assertEquals(session.expiresIn, client.auth.currentSessionOrNull()?.expiresIn) //The session shouldn't be refreshed automatically as alwaysAutoRefresh is false
            client.auth.startAutoRefreshForCurrentSession()
            assertEquals(newSession.expiresIn, client.auth.currentSessionOrNull()?.expiresIn)
        }
    }

    @Test
    fun testGetOAuthUrl() {
        runTest {
            val expectedProvider = Github
            val expectedRedirectUrl = "https://example.com/auth/callback"
            val endpoint = "authorize/custom/endpoint"
            val supabaseUrl = "https://id.supabase.co"
            val client = createMockedSupabaseClient(supabaseUrl = supabaseUrl, configuration = configuration)
            client.auth.awaitInitialization()
            val url = Url(client.auth.getOAuthUrl(expectedProvider, expectedRedirectUrl, endpoint) {
                queryParams["key"] = "value"
                scopes.add("scope1")
                scopes.add("scope2")
            })
            assertEquals(
                endpoint,
                url.pathAfterVersion().substring(1)
            )
            assertEquals(
                expectedProvider.name,
                url.parameters["provider"]
            )
            assertEquals(
                expectedRedirectUrl,
                url.parameters["redirect_to"]
            )
            assertEquals(
                "value",
                url.parameters["key"]
            )
            assertEquals(
                "scope1 scope2",
                url.parameters["scopes"]
            )
        }

    }

    @Test
    fun testSessionMethods() {
        runTest {
            val client = createMockedSupabaseClient(configuration = configuration)
            client.auth.awaitInitialization()
            val expectedIdentities = listOf(Identity("id", buildJsonObject {  }, provider = "provider", userId = "userId"))
            val expectedUser = UserInfo(
                id = "id",
                aud = "aud",
                identities = expectedIdentities
            )
            val session = UserSession(
                accessToken = TEST_JWT,
                refreshToken = "refreshToken",
                expiresIn = 3600,
                tokenType = "Bearer",
                user = expectedUser
            )
            client.auth.importSession(session)
            assertEquals(session, (client.auth.sessionStatus.value as SessionStatus.Authenticated).session)
            assertEquals(session, client.auth.currentSessionOrNull())
            assertEquals(expectedUser, client.auth.currentUserOrNull())
            assertEquals(expectedIdentities, client.auth.currentIdentitiesOrNull())
            client.auth.clearSession()
            assertNull(client.auth.currentSessionOrNull())
        }
    }

    @Test
    fun testClearSession() {
        runTest {
            val client = createMockedSupabaseClient(configuration = configuration)
            client.auth.awaitInitialization()
            val session = userSession()
            client.auth.importSession(session)
            assertIs<SessionStatus.Authenticated>(client.auth.sessionStatus.value)
            client.auth.clearSession()
            assertIs<SessionStatus.NotAuthenticated>(client.auth.sessionStatus.value)
        }
    }

}

fun userSession(customToken: String = TEST_JWT, expiresIn: Long = 3600, user: UserInfo? = null) = UserSession(
    accessToken = customToken,
    refreshToken = "refreshToken",
    expiresIn = expiresIn,
    tokenType = "Bearer",
    user = user
)