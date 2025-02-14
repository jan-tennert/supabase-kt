package io.github.jan.supabase

import io.github.jan.supabase.logging.LogLevel
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Duration

internal data class SupabaseClientConfig(
    val supabaseUrl: String,
    val supabaseKey: String,
    val defaultLogLevel: LogLevel,
    val networkConfig: SupabaseNetworkConfig,
    val defaultSerializer: SupabaseSerializer,
    val accessToken: AccessTokenProvider?,
    val plugins: Map<String, PluginProvider>
)

internal data class SupabaseNetworkConfig(
    val useHTTPS: Boolean,
    val httpEngine: HttpClientEngine?,
    val httpConfigOverrides: List<HttpConfigOverride>,
    val requestTimeout: Duration
)
