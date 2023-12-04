package io.github.jan.supabase.postgrest.request

import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Returning
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonArray

@PublishedApi
internal data class InsertRequest(
    private val upsert: Boolean = false,
    override val returning: Returning = Returning.Minimal,
    private val count: Count? = null,
    private val ignoreDuplicates: Boolean = false,
    private val defaultToNull: Boolean = false,
    override val body: JsonArray,
    override val urlParams: Map<String, String>,
    override val schema: String,
    override val headers: Headers = Headers.Empty,
) : PostgrestRequest {

    override val method = HttpMethod.Post
    override val prefer = buildList {
        add("return=${returning.identifier}")
        if (upsert) add("resolution=${if (ignoreDuplicates) "ignore" else "merge"}-duplicates")
        if(!defaultToNull) add("missing=default")
        if (count != null) add("count=${count.identifier}")
    }

}
