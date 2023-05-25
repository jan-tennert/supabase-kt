package io.github.jan.supabase.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.plugins.MainConfig
import io.github.jan.supabase.plugins.MainPlugin
import io.github.jan.supabase.plugins.SupabasePluginProvider
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders

/**
 * Adds an apollo graphql client to supabase-kt with all necessary headers automatically managed.
 *
 *
 * This plugin uses the default GraphQL endpoint for supabase projects and adds the `apikey` and `Authorization` headers automatically
 */
sealed interface GraphQL: MainPlugin<GraphQL.Config> {

    /**
     * The apollo client. Customizable via [Config.apolloConfiguration]
     */
    val apolloClient: ApolloClient

    /**
     * Config for the [GraphQL] plugin
     * @param apolloConfiguration custom apollo client configuration
     */
    data class Config(
        override var customUrl: String? = null,
        override var jwtToken: String? = null,
        internal var apolloConfiguration: ApolloClient.Builder.() -> Unit = {}
    ): MainConfig {

        /**
         * Add custom apollo client configuration
         */
        fun apolloConfiguration(configuration: ApolloClient.Builder.() -> Unit) {
            apolloConfiguration = configuration
        }

    }

    companion object: SupabasePluginProvider<Config, GraphQL> {

        override val key: String = "graphql"

        /**
         * The current graphql api version
         */
        const val API_VERSION = 1

        override fun create(supabaseClient: SupabaseClient, config: Config): GraphQL {
            return GraphQLImpl(config, supabaseClient)
        }

        override fun createConfig(init: Config.() -> Unit): Config {
            return Config().apply(init)
        }

    }

}

internal class GraphQLImpl(override val config: GraphQL.Config, override val supabaseClient: SupabaseClient) : GraphQL {

    override val apiVersion: Int = GraphQL.API_VERSION
    override val pluginKey: String = GraphQL.key
    override val apolloClient = ApolloClient.Builder().apply {
        serverUrl(config.customUrl ?: resolveUrl())
        addHttpHeader("apikey", supabaseClient.supabaseKey)
        addHttpInterceptor(ApolloHttpInterceptor())
        apply(config.apolloConfiguration)
    }.build()

    override suspend fun parseErrorResponse(response: HttpResponse): RestException {
        throw UnsupportedOperationException("Use apolloClient for graphql requests")
    }

    inner class ApolloHttpInterceptor: HttpInterceptor {
        override suspend fun intercept(
            request: HttpRequest,
            chain: HttpInterceptorChain
        ): com.apollographql.apollo3.api.http.HttpResponse {
            val accessToken = supabaseClient.pluginManager.getPluginOrNull(GoTrue)?.currentAccessTokenOrNull()
            val newRequest = request.newBuilder().apply {
                accessToken?.let {
                    addHeader(HttpHeaders.Authorization, "Bearer $it")
                }
            }
            return chain.proceed(newRequest.build())
        }

    }


}

/**
 * With the [GraphQL] plugin installed, you can access a pre-made apollo client via this property
 */
val SupabaseClient.graphql: GraphQL
    get() = pluginManager.getPlugin(GraphQL)
