package io.github.jan.supabase.postgrest.query.request

import io.github.jan.supabase.postgrest.ColumnRegistry
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder

/**
 * Request builder for [PostgrestQueryBuilder.insert]
 */
open class InsertRequestBuilder(propertyConversionMethod: PropertyConversionMethod, columnRegistry: ColumnRegistry): PostgrestRequestBuilder(propertyConversionMethod, columnRegistry) {

    /**
     * Make missing fields default to `null`.
     * Otherwise, use the default value for the column. This only applies when
     * inserting new rows, not when merging with existing rows under
     */
    var defaultToNull: Boolean = true

}