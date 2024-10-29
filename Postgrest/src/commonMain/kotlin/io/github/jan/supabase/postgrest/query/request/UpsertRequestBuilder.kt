package io.github.jan.supabase.postgrest.query.request

import io.github.jan.supabase.postgrest.ColumnRegistry
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

/**
 * Request builder for [PostgrestQueryBuilder.upsert]
 */
class UpsertRequestBuilder(
    propertyConversionMethod: PropertyConversionMethod,
    columnRegistry: ColumnRegistry
): InsertRequestBuilder(propertyConversionMethod, columnRegistry) {

    /**
     * Comma-separated UNIQUE column(s) to specify how
     * duplicate rows are determined. Two rows are duplicates if all the
     * `onConflict` columns are equal.
     */
    var onConflict: String? = null

    /**
     * If `true`, duplicate rows are ignored. If `false`, duplicate rows are merged with existing rows.
     */
    var ignoreDuplicates: Boolean = false

}