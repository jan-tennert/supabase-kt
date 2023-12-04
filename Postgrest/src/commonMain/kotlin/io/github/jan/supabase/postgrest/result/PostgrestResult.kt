package io.github.jan.supabase.postgrest.result

import io.github.jan.supabase.decode
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder
import io.ktor.http.Headers

/**
 * Represents the result from a postgrest request
 * @param data The data of the response.
 * @param headers The headers of the response
 */
class PostgrestResult(val data: String, val headers: Headers, @PublishedApi internal val postgrest: Postgrest) {

    private val contentRange = headers["Content-Range"]

    /**
     * Returns the total amount of items in the database, or null if no [Count] option was set in the [PostgrestRequestBuilder]
     */
    fun countOrNull(): Long? = contentRange?.substringAfter("/")?.toLongOrNull()

    /**
     * Returns the range of items returned
     */
    fun rangeOrNull(): LongRange? = contentRange?.substringBefore("/")?.let {
        val (start, end) = it.split("-")
        LongRange(start.toLong(), end.toLong())
    }

    /**
     * Decodes [data] as [T] using
     */
    inline fun <reified T : Any> decodeAs(): T = postgrest.serializer.decode(data)

    /**
     * Decodes [data] as [T] using. If there's an error it will return null
     */
    inline fun <reified T : Any> decodeAsOrNull(): T? = try {
        decodeAs()
    } catch (e: Exception) {
        null
    }

    /**
     * Decodes [data] as a list of [T]
     */
    inline fun <reified T : Any> decodeList(): List<T> = decodeAs()

    /**
     * Decodes [data] as a list of [T] and returns the first item found
     */
    inline fun <reified T : Any> decodeSingle(): T = decodeList<T>().first()

    /**
     * Decodes [data] as a list of [T] and returns the first item found or null
     */
    inline fun <reified T : Any> decodeSingleOrNull(): T? = decodeList<T>().firstOrNull()

    /**
     * Returns the data
     */
    operator fun component1() = data

    /**
     * Returns the headers
     */
    operator fun component2() = headers

}
