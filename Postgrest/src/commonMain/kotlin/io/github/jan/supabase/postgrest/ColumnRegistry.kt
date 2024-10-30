package io.github.jan.supabase.postgrest

import io.github.jan.supabase.annotations.SupabaseInternal
import kotlin.reflect.KClass

@SupabaseInternal
class ColumnRegistry(
    private val map: MutableMap<String, String> = mutableMapOf()
) {

    fun <T : Any> getColumns(kClass: KClass<T>): String = map[kClass.simpleName] ?: error("No columns registered for $kClass")

    fun registerColumns(qualifiedName: String, columns: String) {
        map[qualifiedName] = columns
    }

}