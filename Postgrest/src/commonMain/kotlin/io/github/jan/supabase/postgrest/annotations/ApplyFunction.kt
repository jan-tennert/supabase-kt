package io.github.jan.supabase.postgrest.annotations

import io.github.jan.supabase.annotations.SupabaseInternal

/**
 * Annotation to apply a function to a column in a PostgREST query.
 * @param function The function to apply to the column.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class ApplyFunction(val function: String) {

    companion object {
        @SupabaseInternal const val FUNCTION_PARAMETER_NAME = "function"
        const val AVG = "avg"
        const val COUNT = "count"
        const val MAX = "max"
        const val MIN = "min"
        const val SUM = "sum"
    }

}
