package io.github.jan.supabase.postgrest

import io.github.jan.supabase.annotations.SupabaseInternal
import kotlin.reflect.KClass

/**
 * Registry used to map generated columns to a class
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SupabaseInternal
@SubclassOptInRequired(SupabaseInternal::class)
interface ColumnRegistry {

    fun <T : Any> getColumns(kClass: KClass<T>): String

    fun registerColumns(name: String, columns: String)

}

@SupabaseInternal
class MapColumnRegistry(
    private val map: MutableMap<String, String> = mutableMapOf()
): ColumnRegistry {

    override fun <T : Any> getColumns(kClass: KClass<T>): String = map[kClass.simpleName] ?: error("No columns registered for $kClass")

    override fun registerColumns(name: String, columns: String) {
        map[name] = columns
    }

}