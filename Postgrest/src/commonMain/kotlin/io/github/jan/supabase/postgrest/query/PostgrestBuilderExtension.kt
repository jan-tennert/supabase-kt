package io.github.jan.supabase.postgrest.query

import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.PostgrestFilterDSL
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.annotations.Selectable
import io.github.jan.supabase.postgrest.query.request.SelectRequestBuilder
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.ktor.client.plugins.HttpRequestTimeoutException

/**
 * Executes vertical filtering with select on [PostgrestQueryBuilder.table].
 *
 * - This method is a shorthand for [select] with the columns automatically determined by the [T] type.
 * - [T] must be marked with [Selectable] and the `ksp-compiler` KSP dependency must be added to the project (via the KSP Gradle plugin).
 *
 * @param request Additional configurations for the request including filters
 * @return PostgrestResult which is either an error, an empty JsonArray or the data you requested as an JsonArray
 * @throws RestException or one of its subclasses if receiving an error response
 * @throws HttpRequestTimeoutException if the request timed out
 * @throws HttpRequestException on network related issues
 */
@SupabaseExperimental
suspend inline fun <reified T : Any> PostgrestQueryBuilder.select(
    request: @PostgrestFilterDSL SelectRequestBuilder.() -> Unit = {}
): PostgrestResult {
    val registry = postgrest.config.columnRegistry
    val columns = registry.getColumns(T::class)
    return select(Columns.raw(columns), request)
}

/**
 * Return `data` after the query has been executed.
 *
 * - This method is a shorthand for [select] with the columns automatically determined by the [T] type.
 * - [T] must be marked with [Selectable] and the `ksp-compiler` KSP dependency must be added to the project (via the KSP Gradle plugin).
 */
@SupabaseExperimental
inline fun <reified T : Any> PostgrestRequestBuilder.select() {
    val columns = columnRegistry.getColumns(T::class)
    select(Columns.raw(columns))
}
