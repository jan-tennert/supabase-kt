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

        /**
         * Apply the `avg` function to a column.
         */
        const val AVG = "avg"

        /**
         * Apply the `count` function to a column.
         */
        const val COUNT = "count"

        /**
         * Apply the `max` function to a column.
         */
        const val MAX = "max"

        /**
         * Apply the `min` function to a column.
         */
        const val MIN = "min"

        /**
         * Apply the `sum` function to a column.
         */
        const val SUM = "sum"
    }

}
