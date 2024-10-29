package io.github.jan.supabase.postgrest.query.request

import io.github.jan.supabase.postgrest.ColumnRegistry
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder

/**
 * Request builder for [PostgrestQueryBuilder.select]
 */
class SelectRequestBuilder(
    propertyConversionMethod: PropertyConversionMethod,
    columnRegistry: ColumnRegistry
): PostgrestRequestBuilder(propertyConversionMethod, columnRegistry) {

    /**
     * If true, no body will be returned. Useful when using count.
     */
    var head: Boolean = false

}