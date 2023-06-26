package io.github.jan.supabase.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.jan.supabase.SupabaseSerializer
import kotlin.reflect.KType
import kotlin.reflect.javaType

/**
 * A [SupabaseSerializer] that uses jackson-module-kotlin
 */
class JacksonSerializer(private val mapper: ObjectMapper = jacksonObjectMapper()) : SupabaseSerializer {

    override fun <T : Any> encode(type: KType, value: T): String = mapper.writeValueAsString(value)

    @OptIn(ExperimentalStdlibApi::class)
    override fun <T : Any> decode(type: KType, value: String): T = mapper.readValue(value, mapper.constructType(type.javaType))

}