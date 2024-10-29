package io.github.jan.supabase.postgrest

import kotlin.reflect.KClass

class ColumnRegistry(
    private val map: MutableMap<String, String> = mutableMapOf()
) {

    fun <T : Any> getColumns(kClass: KClass<T>): String = map[kClass.qualifiedName] ?: error("No columns registered for $kClass")

    fun registerColumns(qualifiedName: String, columns: String) {
        map[qualifiedName] = columns
    }

}