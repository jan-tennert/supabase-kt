package io.github.jan.supabase.postgrest.annotations

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
 * data class Example(@JsonPath("data", "id") val id: Int)
 * ```
 *
 * @param path The path to the JSON property.
 * @param returnAsText Whether to return the JSON property as text. If `true`, the JSON property will be returned as a string. If `false`, the JSON property will be returned as JSON.
 *
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class JsonPath(vararg val path: String, val returnAsText: Boolean = false) {

    companion object {
        const val PATH_PARAMETER_NAME = "path"
        const val RETURN_AS_TEXT_PARAMETER_NAME = "returnAsText"
    }

}
