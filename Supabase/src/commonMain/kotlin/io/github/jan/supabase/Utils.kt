package io.github.jan.supabase

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.exceptions.SupabaseEncodingException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.decodeFromJsonElement

@SupabaseInternal
val supabaseJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

@OptIn(ExperimentalSerializationApi::class)
@SupabaseInternal
suspend inline fun <reified T> HttpResponse.safeBody(context: String? = null): T {
    val text = bodyAsText()
    val contextMessage = if(context != null) " in $context" else ""
    return try {
        supabaseJson.decodeFromString(text)
    } catch(e: MissingFieldException) {
        throw SupabaseEncodingException("Couldn't decode payload$contextMessage as ${T::class.simpleName}. Input: ${text.replace("\n", "")}")
    }
}

@SupabaseInternal
inline fun buildUrl(baseUrl: String, init: URLBuilder.() -> Unit): String {
    val builder = URLBuilder(baseUrl)
    builder.init()
    return builder.buildString()
}

@SupabaseInternal
fun String.toJsonObject(): JsonObject = supabaseJson.decodeFromString(this)

@SupabaseInternal
fun JsonObjectBuilder.putJsonObject(jsonObject: JsonObject) {
    for (key in jsonObject.keys) {
        put(key, jsonObject[key]!!)
    }
}

@SupabaseInternal
inline fun <reified T> JsonObject.decodeIfNotEmptyOrDefault(default: T): T {
    return if(isEmpty()) {
        default
    } else {
        supabaseJson.decodeFromJsonElement<T>(this)
    }
}

@SupabaseInternal
suspend inline fun <reified T> HttpResponse.bodyOrNull(): T? {
    return try {
        val text = bodyAsText()
        supabaseJson.decodeFromString<T>(text)
    } catch(e: Exception) {
        null
    }
}