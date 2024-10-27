package io.github.jan.supabase.postgrest.annotations

import io.github.jan.supabase.annotations.SupabaseInternal

/**
 * Annotation to cast a column in a PostgREST query.
 * @param type The type to cast the column to. If empty, the type will be inferred from the parameter type. For example, if the parameter is of type `String`, the column will be cast to `text`.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Cast(val type: String = "") {

    companion object {
        @SupabaseInternal const val TYPE_PARAMETER_NAME = "type"
    }

}
