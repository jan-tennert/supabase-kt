package io.github.jan.supabase.postgrest.query.request

import io.github.jan.supabase.postgrest.ColumnRegistry
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.RpcMethod
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder

/**
 * Request builder for [Postgrest.rpc]
 */
class RpcRequestBuilder(
    defaultSchema: String,
    propertyConversionMethod: PropertyConversionMethod,
    columnRegistry: ColumnRegistry
): PostgrestRequestBuilder(propertyConversionMethod, columnRegistry) {

    /**
     * The HTTP method to use. Default is POST
     */
    var method: RpcMethod = RpcMethod.POST

    /**
     * The database schema
     */
    var schema: String = defaultSchema

}