package io.github.jan.supabase.ksp

val primitiveColumnTypes = mapOf<String, String>(
    "kotlin.String" to "text",
    "kotlin.Int" to "int",
    "kotlin.Long" to "int8",
    "kotlin.Float" to "float4",
    "kotlin.Double" to "float8",
    "kotlin.Boolean" to "bool",
    "kotlin.Byte" to "int2",
    "kotlin.Short" to "int2",
    "kotlin.Char" to "char",
    //TIMESTAMPS etc.
)

fun isPrimitive(qualifiedName: String) = primitiveColumnTypes.containsKey(qualifiedName)