import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.PKCEConstants
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.auth.status.SessionSource
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthRequestTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalSettings()
            flowType = FlowType.PKCE
        }
    }

    @Test
    fun testSignUpWithEmailNoAutoconfirm() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedPassword = "password"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val expectedUrl = "https://example.com"
            val client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respondJson(
                    sampleUserObject(email = expectedEmail)
                )
            }
            val user = client.auth.signUpWith(Email, redirectUrl = expectedUrl) {
                email = expectedEmail
                password = expectedPassword
                this.captchaToken = captchaToken
                data = userData
            }
            assertEquals(expectedEmail, user?.email, "Email should be equal")
        }
    }

    @Test
    fun testSignUpWithEmailAutoconfirm() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedPassword = "password"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respondJson(
                    sampleUserSession()
                )
            }
            val user = client.auth.signUpWith(Email) {
                email = expectedEmail
                password = expectedPassword
                this.captchaToken = captchaToken
                data = userData
            }
            assertNull(user)
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            assertEquals(client.auth.sessionSource(), SessionSource.SignUp(Email))
        }
    }

    @Test
    fun testSignUpWithPhoneAutoconfirm() {
        runTest {
            val expectedPhone = "+1234567890"
            val expectedPassword = "password"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respondJson(
                    sampleUserSession()
                )
            }
            val user = client.auth.signUpWith(Phone) {
                phone = expectedPhone
                password = expectedPassword
                this.captchaToken = captchaToken
                data = userData
            }
            assertNull(user)
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            assertEquals(client.auth.sessionSource(), SessionSource.SignUp(Phone))
        }
    }

    @Test
    fun testSignUpWithPhoneNoAutoconfirm() {
        runTest {
            val expectedPhone = "+1234567890"
            val expectedPassword = "password"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val expectedUrl = "https://example.com"
            val client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respondJson(
                    sampleUserObject(phone = expectedPhone)
                )
            }
            val user = client.auth.signUpWith(Phone, redirectUrl = expectedUrl) {
                phone = expectedPhone
                password = expectedPassword
                this.captchaToken = captchaToken
                data = userData
            }
            assertEquals(expectedPhone, user?.phone, "Phone should be equal")
        }
    }

    @Test
    fun testSignUpOtpWithPhone() {
        runTest {
            val expectedPhone = "+1234567890"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val expectedUrl = "https://example.com"
            val client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/otp", it.url.pathAfterVersion())
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respond("")
            }
            client.auth.signUpWith(OTP, redirectUrl = expectedUrl) {
                phone = expectedPhone
                this.captchaToken = captchaToken
                data = userData
            }
        }
    }

    @Test
    fun testSignUpOtpWithEmail() {
        runTest {
            val expectedEmail = "example@email.com"
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val expectedUrl = "https://example.com"
            val client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertEquals(expectedUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/otp", it.url.pathAfterVersion())
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                containsCodeChallenge(body)
                respond("")
            }
            client.auth.signUpWith(OTP, redirectUrl = expectedUrl) {
                email = expectedEmail
                this.captchaToken = captchaToken
                data = userData
            }
        }
    }

    @Test
    fun testSignInWithIDToken() {
        runTest {
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val expectedIdToken = "idToken"
            val expectedProvider = Google
            val expectedAccessToken = "accessToken"
            val expectedNonce = "nonce"
            val client = createMockedSupabaseClient(configuration = configuration) {
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                val params = it.url.parameters
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/token", it.url.pathAfterVersion())
                assertEquals("id_token", params["grant_type"])
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                assertEquals(expectedIdToken, body["id_token"]?.jsonPrimitive?.content)
                assertEquals(expectedProvider.name, body["provider"]?.jsonPrimitive?.content)
                assertEquals(expectedAccessToken, body["access_token"]?.jsonPrimitive?.content)
                assertEquals(expectedNonce, body["nonce"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserSession()
                )
            }
            client.auth.signInWith(IDToken) {
                this.captchaToken = captchaToken
                data = userData
                this.idToken = expectedIdToken
                provider = expectedProvider
                this.nonce = expectedNonce
                accessToken = expectedAccessToken
            }
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            assertEquals(client.auth.sessionSource(), SessionSource.SignIn(IDToken))
        }
    }

    @Test
    fun testSignInAnonymously() {
        runTest {
            val captchaToken = "captchaToken"
            val userData = buildJsonObject {
                put("key", "value")
            }
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/signup", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(captchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(userData, body["data"]!!.jsonObject)
                respondJson(
                    sampleUserSession()
                )
            }
            client.auth.signInAnonymously(
                captchaToken = captchaToken,
                data = userData
            )
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            assertEquals(client.auth.sessionSource(), SessionSource.AnonymousSignIn)
        }
    }

    @Test
    fun testLinkIdentity() {
        runTest {
            val expectedProvider = Google
            val expectedRedirectUrl = "https://example.com"
            val expectedScopes = listOf("scope1", "scope2")
            val expectedUrlParams = mapOf("key" to "value")
            val client = createMockedSupabaseClient(configuration = configuration) {
                val params = it.url.parameters
                assertEquals(expectedRedirectUrl, params["redirect_to"])
                assertMethodIs(HttpMethod.Get, it.method)
                assertPathIs("/user/identities/authorize", it.url.pathAfterVersion())
                assertEquals(expectedProvider.name, params["provider"])
                assertNotNull(params["code_challenge"])
                assertEquals(PKCEConstants.CHALLENGE_METHOD, params["code_challenge_method"])
                assertEquals(expectedScopes.joinToString(" "), params["scopes"])
                assertEquals("value", params["key"])
                respondJson(
                    sampleUserSession()
                )
            }
            client.auth.linkIdentity(expectedProvider, redirectUrl = expectedRedirectUrl) {
                scopes.addAll(expectedScopes)
                queryParams.putAll(expectedUrlParams)
                automaticallyOpenUrl = false
            }
        }
    }

    @Test
    fun testUnlinkIdentity() {
        runTest {
            val expectedIdentityId = "identityId"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Delete, it.method)
                assertPathIs("/user/identities/$expectedIdentityId", it.url.pathAfterVersion())
                respondJson(
                    sampleUserObject()
                )
            }
            client.auth.unlinkIdentity(expectedIdentityId)
        }
    }

    @Test
    fun testRetrieveSSOUrlWithDomain() {
        runTest {
            val expectedRedirectUrl = "https://example.com"
            val expectedDomain = "https://example.com"
            val expectedCaptchaToken = "captchaToken"
            val expectedUrl = "https://ssourl.com"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/sso", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedRedirectUrl, body["redirect_to"]!!.jsonPrimitive.content)
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(expectedDomain, body["domain"]?.jsonPrimitive?.content)
                assertEquals(expectedCaptchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                respondJson(
                    """
                    {
                        "url": "$expectedUrl"
                    }
                    """.trimIndent()
                )
            }
            val result = client.auth.retrieveSSOUrl(redirectUrl = expectedRedirectUrl) {
                this.domain = expectedDomain
                this.captchaToken = expectedCaptchaToken
            }
            assertEquals(expectedUrl, result.url)
        }
    }

    @Test
    fun testRetrieveSSOUrlWithProviderId() {
        runTest {
            val expectedRedirectUrl = "https://example.com"
            val expectedProviderId = "providerId"
            val expectedCaptchaToken = "captchaToken"
            val expectedUrl = "https://ssourl.com"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/sso", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedRedirectUrl, body["redirect_to"]!!.jsonPrimitive.content)
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(expectedProviderId, body["provider_id"]?.jsonPrimitive?.content)
                assertEquals(expectedCaptchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                respondJson(
                    """
                    {
                        "url": "$expectedUrl"
                    }
                    """.trimIndent()
                )
            }
            val result = client.auth.retrieveSSOUrl(redirectUrl = expectedRedirectUrl) {
                this.providerId = expectedProviderId
                this.captchaToken = expectedCaptchaToken
            }
            assertEquals(expectedUrl, result.url)
        }
    }

    @Test
    fun testUpdateUser() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedPhone = "+1234567890"
            val expectedData = buildJsonObject {
                put("key", "value")
            }
            val expectedPassword = "password"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Put, it.method)
                assertPathIs("/user", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(expectedData, body["data"]!!.jsonObject)
                assertEquals(expectedPassword, body["password"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserObject(email = expectedEmail, phone = expectedPhone)
                )
            }
            val user = client.auth.updateUser {
                email = expectedEmail
                phone = expectedPhone
                data = expectedData
                password = expectedPassword
            }
            assertEquals(expectedEmail, user.email, "Email should be equal")
            assertEquals(expectedPhone, user.phone, "Phone should be equal")
        }
    }

    @Test
    fun testResendEmail() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedType = OtpType.Email.SIGNUP
            val expectedCaptchaToken = "captchaToken"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/resend", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(expectedCaptchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserObject(email = expectedEmail)
                )
            }
            client.auth.resendEmail(expectedType, expectedEmail, expectedCaptchaToken)
        }
    }

    @Test
    fun testResendPhone() {
        runTest {
            val expectedPhone = "+1234567890"
            val expectedType = OtpType.Phone.PHONE_CHANGE
            val expectedCaptchaToken = "captchaToken"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/resend", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(expectedCaptchaToken, metaSecurity["captcha_token"]?.jsonPrimitive?.content)
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserObject(email = expectedPhone)
                )
            }
            client.auth.resendPhone(expectedType, expectedPhone, expectedCaptchaToken)
        }
    }

    @Test
    fun testResetPasswordForEmail() {
        runTest {
            val expectedEmail = "example@email.com"
            val expectedCaptchaToken = "captchaToken"
            val expectedRedirectUrl = "https://example.com"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/recover", it.url.pathAfterVersion())
                val params = it.url.parameters
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(
                    expectedCaptchaToken,
                    metaSecurity["captcha_token"]?.jsonPrimitive?.content
                )
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedRedirectUrl, params["redirect_to"])
                containsCodeChallenge(body)
                respondJson(
                    sampleUserObject(email = expectedEmail)
                )
            }
            client.auth.resetPasswordForEmail(expectedEmail, expectedRedirectUrl, expectedCaptchaToken)
        }
    }

    @Test
    fun testReauthenticate() {
        runTest {
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Get, it.method)
                assertPathIs("/reauthenticate", it.url.pathAfterVersion())
                respond("")
            }
            client.auth.reauthenticate()
        }
    }

    @Test
    fun testVerifyEmailOtp() {
        runTest {
            val expectedType = OtpType.Email.EMAIL
            val expectedToken = "token"
            val expectedEmail = "example@email.com"
            val expectedCaptchaToken = "captchaToken"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/verify", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(
                    expectedCaptchaToken,
                    metaSecurity["captcha_token"]?.jsonPrimitive?.content
                )
                assertEquals(expectedToken, body["token"]?.jsonPrimitive?.content)
                assertEquals(expectedEmail, body["email"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserSession()
                )
            }
            client.auth.verifyEmailOtp(expectedType, expectedEmail, expectedToken, expectedCaptchaToken)
        }
    }

    @Test
    fun testVerifyEmailOtpWithTokenHash() {
        runTest {
            val expectedType = OtpType.Email.EMAIL
            val expectedTokenHash = "hash"
            val expectedCaptchaToken = "captchaToken"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/verify", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(
                    expectedCaptchaToken,
                    metaSecurity["captcha_token"]?.jsonPrimitive?.content
                )
                assertEquals(expectedTokenHash, body["token_hash"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserSession()
                )
            }
            client.auth.verifyEmailOtp(expectedType, tokenHash = expectedTokenHash, captchaToken = expectedCaptchaToken)
        }
    }

    @Test
    fun testVerifyPhoneOtp() {
        runTest {
            val expectedType = OtpType.Phone.SMS
            val expectedToken = "token"
            val expectedPhone = "+1234567890"
            val expectedCaptchaToken = "captchaToken"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/verify", it.url.pathAfterVersion())
                val body = it.body.toJsonElement().jsonObject
                val metaSecurity = body["gotrue_meta_security"]!!.jsonObject
                assertEquals(
                    expectedCaptchaToken,
                    metaSecurity["captcha_token"]?.jsonPrimitive?.content
                )
                assertEquals(expectedToken, body["token"]?.jsonPrimitive?.content)
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                assertEquals(expectedType.name.lowercase(), body["type"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserSession()
                )
            }
            client.auth.verifyPhoneOtp(expectedType, expectedPhone, expectedToken, expectedCaptchaToken)
        }
    }

    @Test
    fun testRetrieveUser() {
        runTest {
            val expectedJWT = TEST_JWT
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Get, it.method)
                assertPathIs("/user", it.url.pathAfterVersion())
                assertEquals("Bearer $expectedJWT", it.headers["Authorization"])
                respondJson(
                    sampleUserObject()
                )
            }
            val user = client.auth.retrieveUser(expectedJWT)
            assertNotNull(user, "User should not be null")
        }
    }

    @Test
    fun testSignOut() {
        runTest {
            val expectedScope = SignOutScope.LOCAL
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/logout", it.url.pathAfterVersion())
                val parameters = it.url.parameters
                assertEquals(expectedScope.name.lowercase(), parameters["scope"])
                respond("")
            }
            client.auth.importSession(Json.decodeFromString(sampleUserSession()))
            assertNotNull(client.auth.currentSessionOrNull(), "Session should not be null")
            client.auth.signOut(expectedScope)
            assertNull(client.auth.currentSessionOrNull(), "Session should be null")
        }
    }

    @Test
    fun testRefreshSession() {
        runTest {
            val expectedRefreshToken = "refreshToken"
            val client = createMockedSupabaseClient(configuration = configuration) {
                assertMethodIs(HttpMethod.Post, it.method)
                assertPathIs("/token", it.url.pathAfterVersion())
                val parameters = it.url.parameters
                assertEquals("refresh_token", parameters["grant_type"])
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedRefreshToken, body["refresh_token"]?.jsonPrimitive?.content)
                respondJson(
                    sampleUserSession()
                )
            }
            client.auth.refreshSession(expectedRefreshToken)
        }
    }

    private fun sampleUserObject(email: String? = null, phone: String? = null) = """
        {
            "id": "id",
            "aud": "aud",
            "email": "$email",
            "phone": "$phone"
        }
    """.trimIndent()

    private fun sampleUserSession() = """
        {
        "access_token": "$TEST_JWT",
        "refresh_token": "refresh",
        "token_type": "bearer",
        "expires_in": 3600
        }
    """.trimIndent()

    private fun containsCodeChallenge(body: JsonObject) {
        assertNotNull(body["code_challenge"])
        assertEquals(PKCEConstants.CHALLENGE_METHOD, body["code_challenge_method"]?.jsonPrimitive?.content)
    }

}

expect fun AuthConfig.platformSettings()