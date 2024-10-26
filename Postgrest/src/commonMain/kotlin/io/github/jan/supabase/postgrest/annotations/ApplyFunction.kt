package io.github.jan.supabase.postgrest.annotations

/**
 * Annotation to apply a function to a column in a PostgREST query.
 * @param function The function to apply to the column.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class ApplyFunction(val function: String) {

    companion object {
        const val FUNCTION_PARAMETER_NAME = "function"
    }

}
