package io.github.jan.supabase.postgrest.query.request

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.RpcMethod
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder

/**
 * Request builder for [Postgrest.rpc]
 */
class RpcRequestBuilder(
    defaultSchema: String,
    config: Postgrest.Config
): PostgrestRequestBuilder(config) {

    /**
     * The HTTP method to use. Default is POST
     */
    var method: RpcMethod = RpcMethod.POST

    /**
     * The database schema
     */
    var schema: String = defaultSchema

}