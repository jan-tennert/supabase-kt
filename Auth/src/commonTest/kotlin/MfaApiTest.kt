import app.cash.turbine.test
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.mfa.AuthenticatorAssuranceLevel
import io.github.jan.supabase.auth.mfa.FactorType
import io.github.jan.supabase.auth.mfa.MfaStatus
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserMfaFactor
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.testing.TEST_JWT
import io.github.jan.supabase.testing.assertMethodIs
import io.github.jan.supabase.testing.assertPathIs
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.respondJson
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.util.encodeBase64
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MfaApiTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalSettings()
        }
    }

    @Test
    fun testEnrollTOTP() {
        runTest {
            val friendlyName = "My Factor"
            val issuer = "My App"
            val expectedId = "123"
            val expectedSecret = "secret"
            val expectedQrCode = "qrCode"
            val expectedUri = "uri"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/factors", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals(friendlyName, body["friendly_name"]?.jsonPrimitive?.content)
                assertEquals(FactorType.TOTP.value, body["factor_type"]?.jsonPrimitive?.content)
                assertEquals(issuer, body["issuer"]?.jsonPrimitive?.content)
                respondJson("""
                    {
                    "id": "$expectedId",
                    "totp": {
                        "secret": "$expectedSecret",
                        "qr_code": "$expectedQrCode",
                        "uri": "$expectedUri"
                        }
                    }
                """.trimIndent())
            }
            val factor = client.auth.mfa.enroll(FactorType.TOTP, friendlyName) {
                this.issuer = issuer
            }
            assertEquals(expectedId, factor.id)
            assertEquals(expectedSecret, factor.data.secret)
            assertEquals(expectedQrCode, factor.data.qrCode)
            assertEquals(expectedUri, factor.data.uri)
        }
    }

    @Test
    fun testEnrollPhone() {
        runTest {
            val friendlyName = "My Factor"
            val expectedId = "123"
            val expectedPhone = "+491638976913"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/factors", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals(friendlyName, body["friendly_name"]?.jsonPrimitive?.content)
                assertEquals(FactorType.Phone.value, body["factor_type"]?.jsonPrimitive?.content)
                assertEquals(expectedPhone, body["phone"]?.jsonPrimitive?.content)
                respondJson("""
                    {
                    "id": "$expectedId",
                    "phone": "$expectedPhone"
                    }
                """.trimIndent())
            }
            val factor = client.auth.mfa.enroll(FactorType.Phone, friendlyName) {
                phone = expectedPhone
            }
            assertEquals(expectedId, factor.id)
            assertEquals(expectedPhone, factor.data.phone)
        }
    }

    @Test
    fun testCreateChallengeWithChannel() {
        testCreateChallenge(true)
    }

    @Test
    fun testCreateChallengeWithoutChannel() {
        testCreateChallenge(false)
    }

    @Test
    fun testVerifyChallengeAndSave() {
        testVerifyChallenge(true)
    }

    @Test
    fun testVerifyChallengeWithoutSave() {
        testVerifyChallenge(false)
    }

    @Test
    fun testUnenrollFactor() {
        runTest {
            val expectedFactorId = "123"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/factors/$expectedFactorId", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Delete, it.method)
                respondJson("")
            }
            client.auth.mfa.unenroll(expectedFactorId)
        }
    }

    @Test
    fun testGetAALC1() {
        testGetAAL(AuthenticatorAssuranceLevel.AAL1, AuthenticatorAssuranceLevel.AAL1)
    }

    @Test
    fun testGetAALC2() {
        testGetAAL(AuthenticatorAssuranceLevel.AAL1, AuthenticatorAssuranceLevel.AAL2)
    }

    @Test
    fun testGetAALC3() {
        testGetAAL(AuthenticatorAssuranceLevel.AAL2, AuthenticatorAssuranceLevel.AAL2)
    }

    @Test
    fun testRetrieveVerifiedFactors() {
        runTest {
            val expectedFactor = verifiedFactor()
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                respondJson(UserInfo(id = "id", aud = "aud", factors = listOf(expectedFactor)))
            }
            client.auth.importAuthToken(TEST_JWT)
            val factors = client.auth.mfa.retrieveFactorsForCurrentUser()
            assertEquals(1, factors.size)
            assertEquals(expectedFactor, factors.first())
        }
    }

    @Test
    fun testMfaProperties() {
        runTest {
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                respond("")
            }
            client.auth.importSession(createSession(AuthenticatorAssuranceLevel.AAL1, AuthenticatorAssuranceLevel.AAL2))
            assertEquals(MfaStatus(enabled = true, active = false), client.auth.mfa.status)
            client.auth.importSession(createSession(AuthenticatorAssuranceLevel.AAL2, AuthenticatorAssuranceLevel.AAL1))
            assertEquals(MfaStatus(enabled = false, active = true), client.auth.mfa.status)
        }
    }

    @Test
    fun testMfaStatus() {
        runTest {
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                respond("")
            }
           client.auth.mfa.statusFlow.test {
                assertEquals(MfaStatus(enabled = false, active = false), awaitItem())
                client.auth.importSession(createSession(AuthenticatorAssuranceLevel.AAL1, AuthenticatorAssuranceLevel.AAL2))
                assertEquals(MfaStatus(enabled = true, active = false), awaitItem())
                client.auth.importSession(createSession(AuthenticatorAssuranceLevel.AAL2, AuthenticatorAssuranceLevel.AAL1))
                assertEquals(MfaStatus(enabled = false, active = true), awaitItem())
           }
        }
    }

    private fun createSession(
        currentAAL: AuthenticatorAssuranceLevel,
        nextAAL: AuthenticatorAssuranceLevel
    ): UserSession {
        val data = buildJsonObject {
            put("aal", currentAAL.name.lowercase())
        }
        val encoded = data.toString().encodeBase64()
        val token = "$encoded.$encoded.$encoded"
        val client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            respond("")
        }
        val factors = if(nextAAL == AuthenticatorAssuranceLevel.AAL1) emptyList() else listOf(verifiedFactor())
        return userSession(customToken = token, user = UserInfo(id = "id", aud = "aud", factors = factors))
    }

    private fun testGetAAL(
        current: AuthenticatorAssuranceLevel,
        next: AuthenticatorAssuranceLevel
    ) {
        runTest {
            val data = buildJsonObject {
                put("aal", current.name.lowercase())
            }
            val encoded = data.toString().encodeBase64()
            val token = "$encoded.$encoded.$encoded"
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                respond("")
            }
            val factors = if(next == AuthenticatorAssuranceLevel.AAL1) emptyList() else listOf(verifiedFactor())
            client.auth.importSession(userSession(customToken = token, user = UserInfo(id = "id", aud = "aud", factors = factors)))
            val (c, n) = client.auth.mfa.getAuthenticatorAssuranceLevel()
            assertEquals(current.name.lowercase(), c.name.lowercase())
            assertEquals(next.name.lowercase(), n.name.lowercase())
        }
    }

    private fun testVerifyChallenge(saveSession: Boolean) {
        runTest {
            val expectedFactorId = "123"
            val expectedChallengeId = "456"
            val expectedCode = "789"
            val expectedSession = userSession()
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/factors/$expectedFactorId/verify", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                assertEquals(expectedCode, body["code"]?.jsonPrimitive?.content)
                assertEquals(expectedChallengeId, body["challenge_id"]?.jsonPrimitive?.content)
                respondJson(expectedSession)
            }
            val session = client.auth.mfa.verifyChallenge(expectedFactorId, expectedChallengeId, expectedCode, saveSession)
            assertEquals(expectedSession, session)
            if(saveSession) {
                assertEquals(expectedSession, client.auth.currentSessionOrNull())
            } else {
                assertNull(client.auth.currentSessionOrNull())
            }
        }
    }

    private fun testCreateChallenge(withChannel: Boolean) {
        runTest {
            val expectedFactorId = "123"
            val expectedChannel = "sms"
            val expectedChallengeId = "456"
            val expiresAt = 1L
            val client = createMockedSupabaseClient(
                configuration = configuration
            ) {
                assertPathIs("/factors/$expectedFactorId/challenge", it.url.pathAfterVersion())
                assertMethodIs(HttpMethod.Post, it.method)
                val body = it.body.toJsonElement().jsonObject
                if(withChannel) {
                    assertEquals(expectedChannel, body["channel"]?.jsonPrimitive?.content)
                } else {
                    assertNull(body["channel"])
                }
                respondJson("""
                    {
                    "id": "$expectedChallengeId",
                    "type": "phone",
                    "expires_at": "$expiresAt"
                    }
                """.trimIndent())
            }
            val challenge = client.auth.mfa.createChallenge(expectedFactorId, if(withChannel) Phone.Channel.SMS else null)
            assertEquals(expectedChallengeId, challenge.id)
            assertEquals("phone", challenge.factorType)
            assertEquals(expiresAt, challenge.expiresAt.epochSeconds)
        }
    }

    private fun verifiedFactor() = UserMfaFactor("id", Clock.System.now(), Clock.System.now(), "verified", factorType = "")

}