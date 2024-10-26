package io.github.jan.supabase.postgrest.annotations

/**
 * Annotation to specify that a column is a foreign key in a PostgREST query.
 *
 * This annotation may be used in combination with [ColumnName] to specify the name of the foreign key column. The type of the parameter must be marked with [Selectable].
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Foreign
