package io.github.jan.supabase.postgrest.query

/**
 * Used to order the result of a query
 * @param value The value to be used in the query
 */
enum class Order(val value: String) {
    ASCENDING("asc"), DESCENDING("desc");
}
