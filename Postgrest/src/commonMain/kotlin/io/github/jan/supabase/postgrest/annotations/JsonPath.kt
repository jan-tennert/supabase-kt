package io.github.jan.supabase.postgrest.annotations

import io.github.jan.supabase.annotations.SupabaseInternal

/**
 * Annotation to specify a JSON path in a PostgREST query.
 *
 * Example:
 * Table with a JSON column `data`:
 * ```json
 * {
 *    "id": 1
 * }
 * ```
 * ```kotlin
 * @Selectable
 * data class Example(
 *     @ColumnName("data")
 *     @JsonPath("id")
 *     val id: Int
 * )
 * ```
 *
 * @param key The key of the JSON property.
 * @param path Additional path to the JSON property. If the JSON property is nested, you can specify the path to the property.
 * @param returnAsText Whether to return the JSON property as text. If `true`, the JSON property will be returned as a string. If `false`, the JSON property will be returned as JSON.
 *
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class JsonPath(val key: String, vararg val path: String, val returnAsText: Boolean = false) {

    companion object {
        @SupabaseInternal const val PATH_PARAMETER_NAME = "path"
        @SupabaseInternal const val KEY_PARAMETER_NAME = "key"
        @SupabaseInternal const val RETURN_AS_TEXT_PARAMETER_NAME = "returnAsText"
    }

}
