package io.github.jan.supabase.gotrue

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.coroutines.toSuspendSettings
import io.github.jan.supabase.gotrue.user.UserSession
import io.github.jan.supabase.logging.e
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val settingsJson = Json {
    encodeDefaults = true
}

/**
 * A [SessionManager] that uses the [Settings] API.
 *
 * @param settings The [Settings] instance to use. Defaults to [createDefaultSettings].
 * @param key The key to use for saving the session.
 */
class SettingsSessionManager(
    private val settings: Settings = createDefaultSettings(),
    private val key: String = SETTINGS_KEY,
    private val json: Json = settingsJson
) : SessionManager {

    init {
        checkForOldSession()
    }

    private fun checkForOldSession() {
        if (key == SETTINGS_KEY) return

        val oldSession = settings.getStringOrNull(SETTINGS_KEY)
        val newSession = settings.getStringOrNull(key)

        if (oldSession != null && newSession == null) {
            settings.putString(key, oldSession)
            settings.remove(SETTINGS_KEY)
        }
    }

    private val suspendSettings = settings.toSuspendSettings()

    @OptIn(ExperimentalSettingsApi::class)
    override suspend fun saveSession(session: UserSession) {
        suspendSettings.putString(key, json.encodeToString(session))
    }

    @OptIn(ExperimentalSettingsApi::class)
    override suspend fun loadSession(): UserSession? {
        val session = suspendSettings.getStringOrNull(key) ?: return null
        return try {
            json.decodeFromString(session)
        } catch(e: Exception) {
            Auth.logger.e(e) { "Failed to load session" }
            null
        }
    }

    @OptIn(ExperimentalSettingsApi::class)
    override suspend fun deleteSession() {
        suspendSettings.remove(key)
    }

    companion object {

        /**
         * The key used for saving the session
         */
        const val SETTINGS_KEY = "session"

    }

}