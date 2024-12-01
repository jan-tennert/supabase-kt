package io.github.jan.supabase.postgrest

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.authenticatedSupabaseApi
import io.github.jan.supabase.bodyOrNull
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.postgrest.executor.RestRequestExecutor
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.postgrest.query.request.RpcRequestBuilder
import io.github.jan.supabase.postgrest.request.RpcRequest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.JsonObject

internal class PostgrestImpl(override val supabaseClient: SupabaseClient, override val config: Postgrest.Config) : Postgrest {

    override val apiVersion: Int
        get() = Postgrest.API_VERSION

    override val pluginKey: String
        get() = Postgrest.key

    override var serializer = config.serializer ?: supabaseClient.defaultSerializer

    @OptIn(SupabaseInternal::class)
    val api = supabaseClient.authenticatedSupabaseApi(this)

    override fun from(table: String): PostgrestQueryBuilder {
        return PostgrestQueryBuilder(
            postgrest = this,
            table = table,
        )
    }

    override fun from(schema: String, table: String): PostgrestQueryBuilder {
        return PostgrestQueryBuilder(
            postgrest = this,
            table = table,
            schema = schema,
        )
    }

    override suspend fun parseErrorResponse(response: HttpResponse): RestException {
        val body = response.bodyOrNull<PostgrestErrorResponse>() ?: PostgrestErrorResponse("Unknown error")
        return when(response.status) {
            HttpStatusCode.Unauthorized -> UnauthorizedRestException(body.message, response, body.details ?: body.hint)
            HttpStatusCode.NotFound -> NotFoundRestException(body.message, response, body.details ?: body.hint)
            HttpStatusCode.BadRequest -> BadRequestRestException(body.message, response, body.details ?: body.hint)
            else -> UnknownRestException(body.message, response, body.details ?: body.hint)
        }
    }

    override suspend fun rpc(
        function: String,
        parameters: JsonObject,
        request: RpcRequestBuilder.() -> Unit
    ): PostgrestResult = rpcRequest(function, parameters, request)

    override suspend fun rpc(function: String, request: RpcRequestBuilder.() -> Unit): PostgrestResult = rpcRequest(function, null, request)

    private suspend fun rpcRequest(function: String, body: JsonObject? = null, request: RpcRequestBuilder.() -> Unit): PostgrestResult {
        val requestBuilder = RpcRequestBuilder(config.defaultSchema, config).apply(request)
        val urlParams = buildMap {
            putAll(requestBuilder.params.mapToFirstValue())
            if(requestBuilder.method != RpcMethod.POST && body != null) {
                putAll(body.mapValues { it.value.toString() })
            }
        }
        val rpcRequest = RpcRequest(
            method = requestBuilder.method.httpMethod,
            count = requestBuilder.count,
            urlParams = urlParams,
            body = body,
            schema = requestBuilder.schema,
            headers = requestBuilder.headers.build()
        )
        return RestRequestExecutor.execute(postgrest = this, path = "rpc/$function", request = rpcRequest)
    }

}