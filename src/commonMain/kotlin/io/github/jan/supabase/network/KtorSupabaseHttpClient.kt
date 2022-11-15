package io.github.jan.supabase.network

import io.github.jan.supabase.supabaseJson
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json

/**
 * A [SupabaseHttpClient] that uses ktor to send requests
 */
class KtorSupabaseHttpClient(
    private val supabaseKey: String,
    modifiers: List<HttpClientConfig<*>.() -> Unit> = listOf(),
    engine: HttpClientEngine? = null,
): SupabaseHttpClient() {

    private val httpClient =
        if(engine != null) HttpClient(engine) { applyDefaultConfiguration(modifiers) }
        else HttpClient { applyDefaultConfiguration(modifiers) }

    override suspend fun request(url: String, builder: HttpRequestBuilder.() -> Unit): HttpResponse {
        return httpClient.request(url, builder)
    }

    suspend fun webSocketSession(url: String, block: HttpRequestBuilder.() -> Unit = {}) = httpClient.webSocketSession(url, block)

    fun close() = httpClient.close()

    private fun HttpClientConfig<*>.applyDefaultConfiguration(modifiers: List<HttpClientConfig<*>.() -> Unit>) {
        install(DefaultRequest) {
            headers {
                if(supabaseKey.isNotBlank()) {
                    append("apikey", supabaseKey)
                }
            }
            port = 443
        }
        install(ContentNegotiation) {
            json(supabaseJson)
        }
        modifiers.forEach { it.invoke(this) }
    }

}