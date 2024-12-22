package io.github.jan.supabase.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.plugins.MainConfig
import io.github.jan.supabase.plugins.MainPlugin

/**
 * Returns the access token used for requests. The token is resolved in the following order:
 * 1. [jwtToken] if not null
 * 2. [SupabaseClient.resolveAccessToken] if not null
 * 3. [Auth.currentAccessTokenOrNull] if the Auth plugin is installed
 * 4. [SupabaseClient.supabaseKey] if [keyAsFallback] is true
 */
@SupabaseInternal
suspend fun SupabaseClient.resolveAccessToken(
    jwtToken: String? = null,
    keyAsFallback: Boolean = true
): String? {
    val key = if(keyAsFallback && isApiKeyJWT) supabaseKey else null
    return jwtToken ?: accessToken?.invoke()
    ?: pluginManager.getPluginOrNull(Auth)?.currentAccessTokenOrNull() ?: key
}

/**
 * Returns the access token used for requests. The token is resolved in the following order:
 * 1. [MainConfig.jwtToken] if not null
 * 2. [SupabaseClient.resolveAccessToken] if not null
 * 3. [Auth.currentAccessTokenOrNull] if the Auth plugin is installed
 * 4. [SupabaseClient.supabaseKey] if [keyAsFallback] is true
 */
@SupabaseInternal
suspend fun <C : MainConfig> SupabaseClient.resolveAccessToken(
    plugin: MainPlugin<C>,
    keyAsFallback: Boolean = true
) = resolveAccessToken(plugin.config.jwtToken, keyAsFallback)