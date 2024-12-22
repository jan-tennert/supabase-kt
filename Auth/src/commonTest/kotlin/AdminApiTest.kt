import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.admin.LinkType
import io.github.jan.supabase.auth.admin.generateLinkFor
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserMfaFactor
import io.github.jan.supabase.testing.TEST_JWT
import io.github.jan.supabase.testing.assertMethodIs
import io.github.jan.supabase.testing.assertPathIs
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.respondJson
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminApiTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalSettings()
        }
    }

    @Test
    fun testSignOut() {
        runTest {
            val jwt = TEST_JWT
            val scope = SignOutScope.LOCAL
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/logout", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val params = it.url.parameters
                val headers = it.headers
                assertEquals(scope.name.lowercase(), params["scope"])
                assertEquals("Bearer $jwt", headers["Authorization"])
                respond("")
            }
            client.auth.admin.signOut(
                jwt = jwt,
                scope = scope
            )
        }
    }

    @Test
    fun testCreateUserWithEmail() {
        runTest {
            val email = "email"
            val password = "password"
            val userMetadata = buildJsonObject {
                put("type", "user")
            }
            val appMetadata = buildJsonObject {
                put("type", "app")
            }
            val autoConfirm = true
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/users", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals(email, body["email"]?.jsonPrimitive?.content)
                assertEquals(password, body["password"]?.jsonPrimitive?.content)
                assertEquals(userMetadata, body["user_metadata"]?.jsonObject)
                assertEquals(appMetadata, body["app_metadata"]?.jsonObject)
                assertEquals(autoConfirm, body["email_confirm"]?.jsonPrimitive?.boolean)
                respondJson(
                    Json.encodeToString(UserInfo(
                        id = "id",
                        email = email,
                        appMetadata = appMetadata,
                        userMetadata = userMetadata,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                        confirmedAt = Clock.System.now(),
                        aud = "aud",
                    ))
                )
            }
            val newUser = client.auth.admin.createUserWithEmail {
                this.email = email
                this.password = password
                this.userMetadata = userMetadata
                this.appMetadata = appMetadata
                this.autoConfirm = autoConfirm
            }
            assertEquals(email, newUser.email)
            assertEquals(appMetadata, newUser.appMetadata)
            assertEquals(userMetadata, newUser.userMetadata)
        }
    }

    @Test
    fun testCreateUserWithPhone() {
        runTest {
            val phone = "+123456789"
            val password = "password"
            val userMetadata = buildJsonObject {
                put("type", "user")
            }
            val appMetadata = buildJsonObject {
                put("type", "app")
            }
            val autoConfirm = true
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/users", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals(phone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(password, body["password"]?.jsonPrimitive?.content)
                assertEquals(userMetadata, body["user_metadata"]?.jsonObject)
                assertEquals(appMetadata, body["app_metadata"]?.jsonObject)
                assertEquals(autoConfirm, body["phone_confirm"]?.jsonPrimitive?.boolean)
                respondJson(
                    Json.encodeToString(UserInfo(
                        id = "id",
                        phone = phone,
                        appMetadata = appMetadata,
                        userMetadata = userMetadata,
                        createdAt = Clock.System.now(),
                        updatedAt = Clock.System.now(),
                        confirmedAt = Clock.System.now(),
                        aud = "aud",
                    ))
                )
            }
            val newUser = client.auth.admin.createUserWithPhone {
                this.phone = phone
                this.password = password
                this.userMetadata = userMetadata
                this.appMetadata = appMetadata
                this.autoConfirm = autoConfirm
            }
            assertEquals(phone, newUser.phone)
            assertEquals(appMetadata, newUser.appMetadata)
            assertEquals(userMetadata, newUser.userMetadata)
        }
    }

    @Test
    fun testRetrieveUsers() {
        runTest {
            val page = 3
            val perPage = 50
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/users", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                val params = it.url.parameters
                assertEquals(page.toString(), params["page"])
                assertEquals(perPage.toString(), params["per_page"])
                respondJson(
                    buildJsonObject {
                        put("users",  Json.encodeToJsonElement(
                            listOf(
                                UserInfo(
                                    id = "id",
                                    email = "email",
                                    appMetadata = null,
                                    userMetadata = null,
                                    createdAt = Clock.System.now(),
                                    updatedAt = Clock.System.now(),
                                    confirmedAt = Clock.System.now(),
                                    aud = "aud",
                                )
                            )
                        ))
                    }
                )
            }
            val users = client.auth.admin.retrieveUsers(page = page, perPage = perPage)
            assertEquals(1, users.size)
            assertEquals("email", users.first().email)
        }
    }

    @Test
    fun testRetrieveUserById() {
        runTest {
            val uid = "uid"
            val email = "email"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/users/$uid", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                respondJson(
                    Json.encodeToString(
                        UserInfo(
                            id = uid,
                            email = email,
                            appMetadata = null,
                            userMetadata = null,
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now(),
                            confirmedAt = Clock.System.now(),
                            aud = "aud",
                        )
                    )
                )
            }
            val user = client.auth.admin.retrieveUserById(uid)
            assertEquals(email, user.email)
        }
    }

    @Test
    fun testDeleteUser() {
        runTest {
            val uid = "uid"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/users/$uid", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Delete, it.method)
                respond("")
            }
            client.auth.admin.deleteUser(uid)
        }
    }

    @Test
    fun testInviteUserById() {
        runTest {
            val email = "email"
            val redirectTo = "https://example.com"
            val data = buildJsonObject {
                put("type", "user")
            }
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/invite", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                val params = it.url.parameters
                assertEquals(email, body["email"]?.jsonPrimitive?.content)
                assertEquals(redirectTo, params["redirect_to"])
                assertEquals(data, body["data"]?.jsonObject)
                respond("")
            }
            client.auth.admin.inviteUserByEmail(
                email = email,
                redirectTo = redirectTo,
                data = data
            )
        }
    }

    @Test
    fun testUpdateUserById() {
        runTest {
            val uid = "uid"
            val email = "email"
            val userMetadata = buildJsonObject {
                put("type", "user")
            }
            val appMetadata = buildJsonObject {
                put("type", "app")
            }
            val banDuration = "300ms"
            val role = "admin"
            val phone = "+123456789"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/users/$uid", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Put, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals(email, body["email"]?.jsonPrimitive?.content)
                assertEquals(userMetadata, body["user_metadata"]?.jsonObject)
                assertEquals(appMetadata, body["app_metadata"]?.jsonObject)
                assertEquals(banDuration, body["ban_duration"]?.jsonPrimitive?.content)
                assertEquals(role, body["role"]?.jsonPrimitive?.content)
                assertEquals(phone, body["phone"]?.jsonPrimitive?.content)
                respondJson(
                    Json.encodeToString(
                        UserInfo(
                            id = uid,
                            email = email,
                            appMetadata = appMetadata,
                            userMetadata = userMetadata,
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now(),
                            confirmedAt = Clock.System.now(),
                            phone = phone,
                            aud = "aud",
                        )
                    )
                )
            }
            val updatedUser = client.auth.admin.updateUserById(uid) {
                this.email = email
                this.userMetadata = userMetadata
                this.appMetadata = appMetadata
                this.banDuration = banDuration
                this.role = role
                this.phone = phone
            }
            assertEquals(email, updatedUser.email)
            assertEquals(phone, updatedUser.phone)
            assertEquals(appMetadata, updatedUser.appMetadata)
            assertEquals(userMetadata, updatedUser.userMetadata)
        }
    }

    @Test
    fun testDeleteFactor() {
        runTest {
            val uid = "uid"
            val factorId = "factorId"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/users/$uid/factors/$factorId", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Delete, it.method)
                respond("")
            }
            client.auth.admin.deleteFactor(uid, factorId)
        }
    }

    @Test
    fun testRetrieveFactors() {
        runTest {
            val uid = "uid"
            val factor = UserMfaFactor(
                id = uid,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now(),
                status = "status",
                friendlyName = "friendlyName",
                factorType = "factorType",
            )
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/users/$uid/factors", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Get, it.method)
                respondJson(
                    Json.encodeToJsonElement(
                        listOf(
                            factor
                        )
                    )
                )
            }
            val factors = client.auth.admin.retrieveFactors(uid)
            assertEquals(factor, factors.first())
        }
    }

    @Test
    fun testGenerateLinkFor() {
        runTest {
            val email = "email"
            val redirectTo = "https://example.com"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/admin/generate_link", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                val params = it.url.parameters
                assertEquals("magiclink", body["type"]?.jsonPrimitive?.content)
                assertEquals(email, body["email"]?.jsonPrimitive?.content)
                assertEquals(redirectTo, params["redirect_to"])
                respondJson(
                    Json.encodeToString(
                        UserInfo(
                            id = "id",
                            email = email,
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now(),
                            confirmedAt = Clock.System.now(),
                            aud = "aud",
                            actionLink = "https://example.com"
                        )
                    )
                )
            }
            val (link, user) = client.auth.admin.generateLinkFor(
                linkType = LinkType.MagicLink,
                redirectTo = redirectTo,
                config = {
                    this.email = email
                }
            )
            assertEquals("https://example.com", link)
            assertEquals(email, user.email)
        }
    }

}