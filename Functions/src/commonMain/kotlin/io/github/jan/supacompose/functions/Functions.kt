package io.github.jan.supacompose.functions

import io.github.jan.supacompose.SupabaseClient
import io.github.jan.supacompose.auth.auth
import io.github.jan.supacompose.auth.checkErrors
import io.github.jan.supacompose.auth.currentAccessToken
import io.github.jan.supacompose.buildUrl
import io.github.jan.supacompose.plugins.MainConfig
import io.github.jan.supacompose.plugins.MainPlugin
import io.github.jan.supacompose.plugins.SupacomposePluginProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.appendEncodedPathSegments
import io.ktor.util.appendAll

class Functions(override val config: Config, override val supabaseClient: SupabaseClient) : MainPlugin<Functions.Config> {

    override val API_VERSION: Int
        get() = Functions.API_VERSION

    override val PLUGIN_KEY: String
        get() = key

    private val baseUrl = supabaseClient.supabaseHttpUrl.replaceFirst(".", ".functions.")

    /**
     * Invokes a remote edge function. The authorization token is automatically added to the request.
     * @param function The function to invoke
     * @param builder The request builder to configure the request
     */
    suspend inline operator fun invoke(function: String, builder: HttpRequestBuilder.() -> Unit): HttpResponse {
        return supabaseClient.httpClient.post(resolveUrl(function)) {
            supabaseClient.auth.currentAccessToken()?.let {
                this.headers[HttpHeaders.Authorization] = "Bearer $it"
            }
            builder()
        }.checkErrors("Couldn't invoke function $function")
    }

    /**
     * Invokes a remote edge function. The authorization token is automatically added to the request.
     * Note, if you want to serialize [body] to json, you need to add the [HttpHeaders.ContentType] header yourself.
     * @param function The function to invoke
     * @param body The body of the request
     * @param headers Headers to add to the request
     */
    suspend inline operator fun <reified T> invoke(function: String, body: T, headers: Headers = Headers.Empty): HttpResponse = invoke(function) {
        this.headers.appendAll(headers)
        body?.let {
            setBody(body)
        }
    }

    /**
     * Invokes a remote edge function. The authorization token is automatically added to the request.
     * @param function The function to invoke
     * @param headers Headers to add to the request
     */
    suspend inline operator fun invoke(function: String, headers: Headers = Headers.Empty): HttpResponse = invoke(function) {
        this.headers.appendAll(headers)
    }

    /**
     * Builds an [EdgeFunction] which can be invoked multiple times
     */
    inline fun buildEdgeFunction(builder: EdgeFunctionBuilder.() -> Unit) = EdgeFunctionBuilder(supabaseClient = supabaseClient).apply(builder).toEdgeFunction()

    override fun resolveUrl(path: String): String {
        return buildUrl(config.customUrl ?: baseUrl) {
            appendEncodedPathSegments(path)
        }
    }

    data class Config(
        override val customUrl: String? = null
    ) : MainConfig

    companion object : SupacomposePluginProvider<Config, Functions> {

        override val key = "functions"
        const val API_VERSION = 1

        override fun create(supabaseClient: SupabaseClient, config: Config): Functions {
            return Functions(config, supabaseClient)
        }

        override fun createConfig(init: Config.() -> Unit) = Config().apply(init)

    }

}

/**
 * The Functions plugin handles everything related to supabase's edge functions
 */
val SupabaseClient.functions: Functions
    get() = pluginManager.getPlugin(Functions.key)