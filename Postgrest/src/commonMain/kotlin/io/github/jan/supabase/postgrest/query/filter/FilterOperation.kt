@file:Suppress(
    "UndocumentedPublicClass",
    "UndocumentedPublicFunction",
    "UndocumentedPublicProperty"
)
package io.github.jan.supabase.postgrest.query.filter

/**
 * Represents a filter operation for a column using a specific operator and a value.
 */
data class FilterOperation(val column: String, val operator: FilterOperator, val value: String)
