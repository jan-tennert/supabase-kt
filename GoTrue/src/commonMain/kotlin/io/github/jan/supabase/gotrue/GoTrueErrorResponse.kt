package io.github.jan.supabase.gotrue

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = GoTrueErrorResponse.Companion::class)
internal data class GoTrueErrorResponse(
    val error: String,
    val description: String? = null,
    val weakPassword: WeakPassword? = null
) {

    @Serializable
    data class WeakPassword(
        val reasons: List<String>
    )

    companion object : KSerializer<GoTrueErrorResponse> {

        override val descriptor = buildClassSerialDescriptor("GoTrueErrorResponse") {
            element("error", String.serializer().descriptor)
        }

        override fun deserialize(decoder: Decoder): GoTrueErrorResponse {
            decoder as JsonDecoder
            val json = decoder.decodeJsonElement()
            val error = json.jsonObject["error_code"]?.jsonPrimitive?.content ?: "unknown_error"
            val description = json.jsonObject["error_description"]?.jsonPrimitive?.content ?: json.jsonObject["msg"]?.jsonPrimitive?.content ?: json.jsonObject["message"]?.jsonPrimitive?.content ?: json.toString()
            val weakPassword = if(json.jsonObject.containsKey("weak_password")) {
                Json.decodeFromJsonElement<WeakPassword>(json.jsonObject["weak_password"]!!)
            } else null
            return GoTrueErrorResponse(error, description, weakPassword)
        }

        override fun serialize(encoder: Encoder, value: GoTrueErrorResponse) {
            throw UnsupportedOperationException()
        }

    }

}