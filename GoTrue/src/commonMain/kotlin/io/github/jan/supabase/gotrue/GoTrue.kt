package io.github.jan.supabase.gotrue

import co.touchlab.kermit.Logger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.encodeToJsonElement
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.admin.AdminApi
import io.github.jan.supabase.gotrue.mfa.MfaApi
import io.github.jan.supabase.gotrue.providers.AuthProvider
import io.github.jan.supabase.gotrue.providers.ExternalAuthConfigDefaults
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.OAuthProvider
import io.github.jan.supabase.gotrue.providers.builtin.DefaultAuthProvider
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.Phone
import io.github.jan.supabase.gotrue.providers.builtin.SSO
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.gotrue.user.UserUpdateBuilder
import io.github.jan.supabase.plugins.MainPlugin
import io.github.jan.supabase.plugins.SupabasePluginProvider
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Plugin to interact with the supabase Auth API
 *
 * To use it you need to install it to the [SupabaseClient]:
 * ```kotlin
 * val client = createSupabaseClient(supabaseUrl, supabaseKey) {
 *    install(GoTrue)
 * }
 * ```
 *
 * then you can use it like this:
 * ```kotlin
 * val result = client.gotrue.signUpWith(Email) {
 *   email = "example@email.com"
 *   password = "password"
 * }
 * ```
 */
sealed interface GoTrue : MainPlugin<GoTrueConfig> {

    /**
     * Returns the current session status
     */
    val sessionStatus: StateFlow<SessionStatus>

    /**
     * Whether the [sessionStatus] session is getting refreshed automatically
     */
    val isAutoRefreshRunning: Boolean

    /**
     * Returns the session manager instance
     */
    val sessionManager: SessionManager

    /**
     * Access to the auth admin api where you can manage users. Service role access token is required. Import it via [importAuthToken]. Never share it publicly
     */
    val admin: AdminApi

    /**
     * Access to the mfa api where you can manage multi-factor authentication for the current user.
     */
    val mfa: MfaApi

    /**
     * The cache for the code verifier. This is used for PKCE authentication. Can be customized via [GoTrueConfig.codeVerifierCache]
     */
    val codeVerifierCache: CodeVerifierCache

    /**
     * Signs up a new user with the specified [provider]
     *
     * Example:
     * ```kotlin
     * val result = gotrue.signUpWith(Email) {
     *    email = "example@email.com"
     *    password = "password"
     * }
     * or
     * gotrue.signUpWith(Google) // Opens the browser to login with google
     * ```
     *
     * @param provider the provider to use for signing up. E.g. [Email], [Phone] or [Google]
     * @param redirectUrl The redirect url to use. If you don't specify this, the platform specific will be use, like deeplinks on android.
     * @param config The configuration to use for the sign-up.
     * @return The result of the sign-up (e.g. the user id) or null if auto-confirm is enabled (resulting in a login)
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun <C, R, Provider : AuthProvider<C, R>> signUpWith(
        provider: Provider,
        redirectUrl: String? = null,
        config: (C.() -> Unit)? = null
    ): R?

    /**
     * Logins the user with the specified [provider]
     *
     * Example:
     * ```kotlin
     * val result = gotrue.loginWith(Email) {
     *    email = "example@email.com"
     *    password = "password"
     * }
     * or
     * gotrue.loginWith(Google) // Opens the browser to login with google
     * ```
     *
     * @param provider the provider to use for signing up. E.g. [Email], [Phone] or [Google]
     * @param redirectUrl The redirect url to use. If you don't specify this, the platform specific will be use, like deeplinks on android.
     * @param config The configuration to use for the sign-up.
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun <C, R, Provider : AuthProvider<C, R>> loginWith(
        provider: Provider,
        redirectUrl: String? = null,
        config: (C.() -> Unit)? = null
    )

    /**
     * Retrieves the sso url for the specified [type]
     * @param type The type of sso to retrieve e.g. [SSO.withDomain] or [SSO.withProvider]
     * @param redirectUrl The redirect url to use
     * @param config The configuration to use
     */
    suspend fun <Config: SSO.Config> retrieveSSOUrl(type: SSO<Config>, redirectUrl: String? = null, config: (Config.() -> Unit)? = null): SSO.Result

    /**
     * Modifies the current user
     * @param updateCurrentUser Whether to update the current user in the [SupabaseClient]
     * @param config The configuration to use
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun modifyUser(
        updateCurrentUser: Boolean = true,
        config: UserUpdateBuilder.() -> Unit
    ): UserInfo

    /**
     * Sends a one time password to the specified [provider]
     *
     * Example:
     * ```kotlin
     * gotrue.sendOtpTo(Email) {
     *    email = "example@email.com"
     *    password = "password"
     * }
     * ```
     *
     * @param provider The provider to use. Either [Email] or [Phone]
     * @param createUser Whether to create a user when a user with the given credentials doesn't exist
     * @param redirectUrl The redirect url to use. If you don't specify this, the platform specific will be use, like deeplinks on android.
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun <C, R, Provider : DefaultAuthProvider<C, R>> sendOtpTo(
        provider: Provider,
        createUser: Boolean = false,
        redirectUrl: String? = null,
        data: JsonObject? = null,
        config: C.() -> Unit
    )

    /**
     * Resends an existing signup confirmation email, email change email
     * @param type The email otp type
     * @param email The email to resend the otp to
     * @param captchaToken The captcha token to use
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun resendEmail(type: OtpType.Email, email: String, captchaToken: String? = null)

    /**
     * Resends an existing SMS OTP or phone change OTP.
     * @param type The phone otp type
     * @param phoneNumber The phone to resend the otp to
     * @param captchaToken The captcha token to use
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun resendPhone(type: OtpType.Phone, phoneNumber: String, captchaToken: String? = null)

    /**
     * Sends a password reset email to the user with the specified [email]
     * @param email The email to send the password reset email to
     * @param redirectUrl The redirect url to use. If you don't specify this, the platform specific will be use, like deeplinks on android.
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun sendRecoveryEmail(email: String, redirectUrl: String? = null, captchaToken: String? = null)

    /**
     * Sends a nonce to the user's email (preferred) or phone
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun reauthenticate()

    /**
     * Revokes all refresh tokens for the user, and invalidates the session
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    @Deprecated("Use logout() instead", ReplaceWith("logout()"))
    suspend fun invalidateAllRefreshTokens()

    /**
     * Verifies a email otp
     * @param type The type of the verification
     * @param email The email to verify
     * @param token The token used to verify
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun verifyEmailOtp(type: OtpType.Email, email: String, token: String, captchaToken: String? = null)

    /**
     * Verifies a phone/sms otp
     * @param type The type of the verification
     * @param token The otp to verify
     * @param phoneNumber The phone number the token was sent to
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun verifyPhoneOtp(type: OtpType.Phone, phoneNumber: String, token: String, captchaToken: String? = null)

    /**
     * Retrieves the user attached to the specified [jwt]
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun retrieveUser(jwt: String): UserInfo

    /**
     * Retrieves the current user with the current session
     * @param updateSession Whether to update [sessionStatus] with the updated user, if [sessionStatus] is [SessionStatus.Authenticated]
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun retrieveUserForCurrentSession(updateSession: Boolean = false): UserInfo

    /**
     * Invalidates the current session, which means [sessionStatus] will be [SessionStatus.NotAuthenticated]
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    @Deprecated("Use logout() instead", ReplaceWith("logout()"))
    suspend fun invalidateSession()

    /**
     * Logs out the current user, which means [sessionStatus] will be [SessionStatus.NotAuthenticated] and the access token will be revoked
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun logout()

    /**
     * Imports a user session and starts auto-refreshing if [autoRefresh] is true
     */
    suspend fun importSession(session: UserSession, autoRefresh: Boolean = true)

    /**
     * Imports the jwt token and retrieves the user profile.
     * Be aware auto-refreshing is not available when importing **only** a jwt token.
     * @param accessToken The jwt token to import
     * @param retrieveUser Whether to retrieve the user profile or not
     */
    suspend fun importAuthToken(accessToken: String, refreshToken: String = "", retrieveUser: Boolean = false, autoRefresh: Boolean = if(refreshToken.isNotBlank()) config.alwaysAutoRefresh else false) = importSession(UserSession(accessToken, refreshToken, "", "", 0L, "", if(retrieveUser) tryToGetUser(accessToken) else null), autoRefresh)

    /**
     * Retrieves the latest session from storage and starts auto-refreshing if [autoRefresh] is true or [GoTrue.Config.alwaysAutoRefresh] as the default parameter
     * @return true, if a session was found, false otherwise
     */
    suspend fun loadFromStorage(autoRefresh: Boolean = config.alwaysAutoRefresh): Boolean

    /**
     * Refreshes a session using the refresh token
     * @param refreshToken The refresh token to use
     * @return A new session
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun refreshSession(refreshToken: String): UserSession

    /**
     * Refreshes the current session
     * @throws RestException or one of its subclasses if receiving an error response
     * @throws HttpRequestTimeoutException if the request timed out
     * @throws HttpRequestException on network related issues
     */
    suspend fun refreshCurrentSession()

    /**
     * Updates the current user with the current access token
     */
    @Deprecated("Use retrieveUserForCurrentSession() instead", ReplaceWith("retrieveUserForCurrentSession(true)"))
    suspend fun updateCurrentUser()

    /**
     * Exchanges a code for a session. Used when using the [FlowType.PKCE] flow
     * @param code The code to exchange
     * @param saveSession Whether to save the session in storage
     */
    suspend fun exchangeCodeForSession(code: String, saveSession: Boolean = true): UserSession

    /**
     * Starts auto-refreshing [session] for [currentSession]
     * @param session The session to auto-refresh
     */
    @Deprecated("Use importSession() instead", ReplaceWith("importSession(session)"))
    suspend fun startAutoRefresh(session: UserSession, autoRefresh: Boolean = config.alwaysAutoRefresh)

    /**
     * Starts auto refreshing the current session
     */
    suspend fun startAutoRefreshForCurrentSession()

    /**
     * Returns the url to use for oAuth
     * @param provider The provider to use
     * @param redirectUrl The redirect url to use
     */
    fun oAuthUrl(provider: OAuthProvider, redirectUrl: String? = null, additionalConfig: ExternalAuthConfigDefaults.() -> Unit = {}): String

    /**
     * Stops auto-refreshing the current session
     */
    fun stopAutoRefreshForCurrentSession()

    /**
     * Returns the current access token, or null if no session is available
     */
    fun currentAccessTokenOrNull() = currentSessionOrNull()?.accessToken

    /**
     * Returns the current session or null
     */
    fun currentSessionOrNull() = when(val status = sessionStatus.value) {
        is SessionStatus.Authenticated -> status.session
        else -> null
    }

    /**
     * Returns the current user or null
     */
    fun currentUserOrNull() = currentSessionOrNull()?.user

    companion object : SupabasePluginProvider<GoTrueConfig, GoTrue> {

        override val key = "auth"

        /**
         * The gotrue api version to use
         */
        const val API_VERSION = 1

        override fun createConfig(init: GoTrueConfig.() -> Unit) = GoTrueConfig().apply(init)
        override fun create(supabaseClient: SupabaseClient, config: GoTrueConfig): GoTrue = GoTrueImpl(supabaseClient, config)

    }

}

/**
 * Sends a one time password to the specified [provider]
 *
 * Example:
 * ```kotlin
 * gotrue.sendOtpTo(Email) {
 *    email = "example@email.com"
 *    password = "password"
 * }
 * ```
 *
 * @param provider The provider to use. Either [Email] or [Phone]
 * @param createUser Whether to create a user when a user with the given credentials doesn't exist
 * @param redirectUrl The redirect url to use. If you don't specify this, the platform specific will be use, like deeplinks on android.
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
suspend inline fun <C, R, reified D : Any, Provider : DefaultAuthProvider<C, R>> GoTrue.sendOtpTo(
    provider: Provider,
    data: D,
    createUser: Boolean = false,
    redirectUrl: String? = null,
    noinline config: C.() -> Unit = { }
): Unit = sendOtpTo(provider, createUser, redirectUrl, this.config.serializer.encodeToJsonElement(data).jsonObject, config)

/**
 * The Auth plugin handles everything related to supabase's authentication system
 */
val SupabaseClient.gotrue: GoTrue
    get() = pluginManager.getPlugin(GoTrue)

private suspend fun GoTrue.tryToGetUser(jwt: String) = try {
    retrieveUser(jwt)
} catch (e: Exception) {
    Logger.e(e) { "Couldn't retrieve user using your custom jwt token. If you use the project secret ignore this message" }
    null
}