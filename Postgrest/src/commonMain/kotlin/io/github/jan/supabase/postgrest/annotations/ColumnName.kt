package io.github.jan.supabase.postgrest.annotations

/**
 * Annotation to specify the name of a column in a PostgREST query.
 *
 * If this annotation is not present, the name of the column will be inferred from the parameter name.
 * @param name The name of the column.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class ColumnName(val name: String) {

    companion object {
        const val NAME_PARAMETER_NAME = "name"
    }

}
