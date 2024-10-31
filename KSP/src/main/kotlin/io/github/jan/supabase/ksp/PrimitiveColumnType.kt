package io.github.jan.supabase.ksp

//Used for auto-casting, when no type is specified
val primitiveColumnTypes = mapOf<String, String>(
    "kotlin.String" to "text",
    "kotlin.Int" to "int4",
    "kotlin.Long" to "int8",
    "kotlin.Float" to "float4",
    "kotlin.Double" to "float8",
    "kotlin.Boolean" to "bool",
    "kotlin.Byte" to "int2",
    "kotlin.Short" to "int2",
    "kotlin.Char" to "char",
    "kotlinx.datetime.Instant" to "timestamptz",
    "kotlinx.datetime.LocalDateTime" to "timestamp",
    "kotlin.uuid.Uuid" to "uuid",
    "kotlinx.datetime.LocalTime" to "time",
    "kotlinx.datetime.LocalDate" to "date",
    "kotlinx.serialization.json.JsonElement" to "jsonb",
    "kotlinx.serialization.json.JsonObject" to "jsonb",
    "kotlinx.serialization.json.JsonArray" to "jsonb",
    "kotlinx.serialization.json.JsonPrimitive" to "jsonb",
)

fun isPrimitive(qualifiedName: String) = primitiveColumnTypes.containsKey(qualifiedName)