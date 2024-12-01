package io.github.jan.supabase.postgrest.query.request

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder

/**
 * Request builder for [PostgrestQueryBuilder.select]
 */
class SelectRequestBuilder(
    config: Postgrest.Config
): PostgrestRequestBuilder(config) {

    /**
     * If true, no body will be returned. Useful when using count.
     */
    var head: Boolean = false

}