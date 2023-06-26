@file:Suppress("UndocumentedPublicProperty")
package io.github.jan.supabase.postgrest.query

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.gotrue.PostgrestFilterDSL
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.formatJoiningFilter
import kotlin.reflect.KProperty1

/**
 * A filter builder for a postgrest query
 */
@PostgrestFilterDSL
class PostgrestFilterBuilder(@PublishedApi internal val propertyConversionMethod: PropertyConversionMethod) {

    @PublishedApi
    internal val _params = mutableMapOf<String, List<String>>()
    val params: Map<String, List<String>>
        get() = _params.toMap()

    /**
     * Adds a negated filter to the query
     */
    fun filterNot(column: String, operator: FilterOperator, value: Any?) {
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("not.${operator.identifier}.$value")
    }

    /**
     * Adds a negated filter to the query
     */
    fun filterNot(operation: FilterOperation) = filterNot(operation.column, operation.operator, operation.value)

    /**
     * Adds a filter to the query
     */
    fun filter(column: String, operator: FilterOperator, value: Any?) {
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("${operator.identifier}.$value")
    }

    /**
     * Adds a filter to the query
     */
    fun filter(operation: FilterOperation) = filter(operation.column, operation.operator, operation.value)

    /**
     * Finds all rows where the value of the [column] is equal to [value]
     */
    fun eq(column: String, value: Any) = filter(column, FilterOperator.EQ, value)

    /**
     * Finds all rows where the value of the [column] is not equal to [value]
     */
    fun neq(column: String, value: Any) = filter(column, FilterOperator.NEQ, value)

    /**
     * Finds all rows where the value of the [column] is greater than [value]
     */
    fun gt(column: String, value: Any) = filter(column, FilterOperator.GT, value)

    /**
     * Finds all rows where the value of the [column] is greater than or equal to [value]
     */
    fun gte(column: String, value: Any) = filter(column, FilterOperator.GTE, value)

    /**
     * Finds all rows where the value of the [column] is less than or equal to [value]
     */
    fun lte(column: String, value: Any) = filter(column, FilterOperator.LTE, value)

    /**
     * Finds all rows where the value of the [column] is less than [value]
     */
    fun lt(column: String, value: Any) = filter(column, FilterOperator.LT, value)

    /**
     * Finds all rows where the value of the [column] matches the specified [pattern]
     */
    fun like(column: String, pattern: String) = filter(column, FilterOperator.LIKE, pattern)

    /**
     * Finds all rows where the value of the [column] matches all of the specified [patterns]
     */
    fun likeAll(column: String, patterns: List<String>) {
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("like(all).{${patterns.joinToString(",")}}")
    }

    /**
     * Finds all rows where the value of the [column] matches any of the specified [patterns]
     */
    fun likeAny(column: String, patterns: List<String>) {
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("like(any).{${patterns.joinToString(",")}}")
    }

    /**
     * Finds all rows where the value of the [column] matches all of the specified [patterns]
     */
    fun ilikeAll(column: String, patterns: List<String>) {
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("ilike(all).{${patterns.joinToString(",")}}")
    }

    /**
     * Finds all rows where the value of the [column] matches any of the specified [patterns] (case-insensitive)
     */
    fun ilikeAny(column: String, patterns: List<String>) {
        val columnValue = params[column] ?: emptyList()
        _params[column] = columnValue + listOf("ilike(any).{${patterns.joinToString(",")}}")
    }

    /**
     * Finds all rows where the value of the [column] matches the specified [pattern] (case-insensitive)
     */
    fun ilike(column: String, pattern: String) = filter(column, FilterOperator.ILIKE, pattern)

    /**
     * Finds all rows where the value of the [column] matches the specified [pattern] using pattern matching
     */
    fun match(column: String, pattern: String) = filter(column, FilterOperator.MATCH, pattern)

    /**
     * Finds all rows where the value of the [column] matches the specified [pattern] using pattern matching (case-insensitive)
     */
    fun imatch(column: String, pattern: String) = filter(column, FilterOperator.IMATCH, pattern)

    /**
     * Finds all rows where the value of the [column] equals to one of these values: null,true,false,unknown
     */
    fun exact(column: String, value: Boolean?) = filter(column, FilterOperator.IS, value)

    /**
     * Finds all rows where the value of the [column] is a member of [values]
     */
    fun isIn(column: String, values: List<Any>) = filter(column, FilterOperator.IN, "(${values.joinToString(",")})")

    /**
     * Finds all rows where the value of the [column] is strictly left of [range]
     */
    fun sl(column: String, range: Pair<Any, Any>) = filter(column, FilterOperator.SL, "(${range.first},${range.second})")

    /**
     * Finds all rows where the value of the [column] is strictly right of [range]
     */
    fun sr(column: String, range: Pair<Any, Any>) = filter(column, FilterOperator.SR, "(${range.first},${range.second})")

    /**
     * Finds all rows where the value of the [column] does not extend to the left of [range]
     */
    fun nxl(column: String, range: Pair<Any, Any>) = filter(column, FilterOperator.NXL, "(${range.first},${range.second})")

    /**
     * Finds all rows where the value of the [column] does not extend to the right of [range]
     */
    fun nxr(column: String, range: Pair<Any, Any>) = filter(column, FilterOperator.NXR, "(${range.first},${range.second})")

    /**
     * Finds all rows where the value of the [column] is strictly left of [range]
     */
    fun rangeLte(column: String, range: Pair<Any, Any>) = nxr(column, range)

    /**
     * Finds all rows where the value of the [column] is strictly right of [range]
     */
    fun rangeGte(column: String, range: Pair<Any, Any>) = nxl(column, range)

    /**
     * Finds all rows where the value of the [column] does not extend to the right of [range]
     */
    fun rangeLt(column: String, range: Pair<Any, Any>) = sl(column, range)

    /**
     * Finds all rows where the value of the [column] does not extend to the right of [range]
     */
    fun rangeGt(column: String, range: Pair<Any, Any>) = sr(column, range)

    /**
     * Finds all rows where the value of the [column] is adjacent to [range]
     */
    fun adjacent(column: String, range: Pair<Any, Any>) = filter(column, FilterOperator.ADJ, "(${range.first},${range.second})")

    /**
     * Adds an `or` condition to the query
     */
    @PostgrestFilterDSL
    inline fun or(negate: Boolean = false, filter: @PostgrestFilterDSL PostgrestFilterBuilder.() -> Unit) {
        val prefix = if(negate) "not." else ""
        _params[prefix + "or"] = listOf(formatJoiningFilter(filter))
    }

    /**
     * Adds an `and` condition to the query
     */
    @PostgrestFilterDSL
    inline fun and(negate: Boolean = false, filter: @PostgrestFilterDSL PostgrestFilterBuilder.() -> Unit) {
        val prefix = if (negate) "not." else ""
        _params[prefix + "and"] = listOf(formatJoiningFilter(filter))
    }

    /**
     * Runs a full text search on [column] with the specified [query] and [textSearchType]
     */
    fun textSearch(column: String, query: String, textSearchType: TextSearchType, config: String? = null): PostgrestFilterBuilder {
        val configPart = if (config === null) "" else "(${config})"
        _params[column] = listOf("${textSearchType.identifier}${configPart}.${query}")
        return this
    }

    /**
     * Orders the result by [column] in the specified [order].
     * @param nullsFirst If true, null values will be ordered first
     * @param foreignTable If the column is from a foreign table, specify the table name here
     */
    fun order(column: String, order: Order, nullsFirst: Boolean = false, foreignTable: String? = null) {
        val key = if (foreignTable == null) "order" else "\"$foreignTable\".order"
        _params[key] = listOf("${column}.${order.value}.${if (nullsFirst) "nullsfirst" else "nullslast"}")
    }

    /**
     * Limits the result to [count] rows
     * @param foreignTable If the column is from a foreign table, specify the table name here
     */
    fun limit(count: Long, foreignTable: String? = null) {
        val key = if (foreignTable == null) "limit" else "\"$foreignTable\".limit"
        _params[key] = listOf(count.toString())
    }

    /**
     * Limits the result to rows from [from] to [to]
     * @param foreignTable If the column is from a foreign table, specify the table name here
     */
    fun range(from: Long, to: Long, foreignTable: String? = null) {
        val keyOffset = if (foreignTable == null) "offset" else "\"$foreignTable\".offset"
        val keyLimit = if (foreignTable == null) "limit" else "\"$foreignTable\".limit"

        _params[keyOffset] = listOf(from.toString())
        _params[keyLimit] = listOf((to - from + 1).toString())
    }

    /**
     * Limits the result to rows from [range.first] to [range.last]
     * @param foreignTable If the column is from a foreign table, specify the table name here
     */
    fun range(range: LongRange, foreignTable: String? = null) = range(range.first, range.last, foreignTable)

    /**
     * Finds all rows where the value of the [column] contains [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun contains(column: String, values: List<Any>) = filter(column, FilterOperator.CS, "{${values.joinToString(",")}}")

    /**
     * Finds all rows where the value of the [column] is contained in [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun contained(column: String, values: List<Any>) = filter(column, FilterOperator.CD, "{${values.joinToString(",")}}")

    /**
     * Finds all rows where the value of the [column] contains [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun cs(column: String, values: List<Any>) = contains(column, values)

    /**
     * Finds all rows where the value of the [column] is contained in [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun cd(column: String, values: List<Any>) = contained(column, values)

    /**
     * Finds all rows where the value of the [column] overlaps with [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun ov(column: String, values: List<Any>) = overlaps(column, values)

    /**
     * Finds all rows where the value of the [column] overlaps with [values]
     *
     * @param column The column name to filter on
     * @param values The values to filter on
     */
    fun overlaps(column: String, values: List<Any>) = filter(column, FilterOperator.OV, "{${values.joinToString(",")}}")

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is equal to [value]
     */
    infix fun <T, V> KProperty1<T, V>.eq(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.EQ, value.toString()))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is not equal to [value]
     */
    infix fun <T, V> KProperty1<T, V>.neq(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.NEQ, value.toString()))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is greater than [value]
     */
    infix fun <T, V> KProperty1<T, V>.gt(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.GT, value.toString()))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is greater than or equal to [value]
     */
    infix fun <T, V> KProperty1<T, V>.gte(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.GTE, value.toString()))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is less than [value]
     */
    infix fun <T, V> KProperty1<T, V>.lt(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.LT, value.toString()))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is less than or equal to [value]
     */
    infix fun <T, V> KProperty1<T, V>.lte(value: V) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.LTE, value.toString()))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] matches the specified [pattern]
     */
    infix fun <T, V> KProperty1<T, V>.like(pattern: String) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.LIKE, pattern))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] matches the specified [pattern]
     * using pattern matching
     */
    infix fun <T, V> KProperty1<T, V>.match(pattern: String) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.MATCH, pattern))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] matches the specified [pattern] (case-insensitive)
     */
    infix fun <T, V> KProperty1<T, V>.ilike(pattern: String) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.ILIKE, pattern))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] matches the specified [pattern]
     * using pattern matching
     */
    infix fun <T, V> KProperty1<T, V>.imatch(pattern: String) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.IMATCH, pattern))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] equals to one of these values: null,true,false,unknown
     */
    infix fun <T, V> KProperty1<T, V>.isExact(value: Boolean?) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.IS, value.toString()))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is in the specified [list]
     */
    infix fun <T, V> KProperty1<T, V>.isIn(list: List<V>) = filter(FilterOperation(propertyConversionMethod(this), FilterOperator.IN, "(${list.joinToString(",")})"))

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is strictly left of [range]
     */
    infix fun <T, V> KProperty1<T, V>.rangeLt(range: Pair<Any, Any>) = this@PostgrestFilterBuilder.rangeLt(propertyConversionMethod(this), range)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] does not extend to the left of [range]
     */
    infix fun <T, V> KProperty1<T, V>.rangeLte(range: Pair<Any, Any>) = this@PostgrestFilterBuilder.rangeLte(propertyConversionMethod(this), range)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] does not extend to the right of [range]
     */
    infix fun <T, V> KProperty1<T, V>.rangeGt(range: Pair<Any, Any>) = this@PostgrestFilterBuilder.rangeGt(propertyConversionMethod(this), range)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] does not strictly right of [range]
     */
    infix fun <T, V> KProperty1<T, V>.rangeGte(range: Pair<Any, Any>) = this@PostgrestFilterBuilder.rangeGte(propertyConversionMethod(this), range)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is adjacent to the specified [range]
     */
    infix fun <T, V> KProperty1<T, V>.adjacent(range: Pair<Any, Any>) = this@PostgrestFilterBuilder.adjacent(propertyConversionMethod(this), range)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] overlaps with [values]
     */
    infix fun <T, V> KProperty1<T, V>.overlaps(values: List<Any>) = this@PostgrestFilterBuilder.overlaps(propertyConversionMethod(this), values)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] contains [values]
     */
    infix fun <T, V> KProperty1<T, V>.contains(values: List<Any>) = this@PostgrestFilterBuilder.contains(propertyConversionMethod(this), values)

    /**
     * Finds all rows where the value of the column with the name of the [KProperty1] converted using [propertyConversionMethod] is contained in [values]
     */
    infix fun <T, V> KProperty1<T, V>.contained(values: List<Any>) = this@PostgrestFilterBuilder.contained(propertyConversionMethod(this), values)

}

@SupabaseInternal
inline fun buildPostgrestFilter(propertyConversionMethod: PropertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE, block: PostgrestFilterBuilder.() -> Unit): Map<String, List<String>> {
    val filter = PostgrestFilterBuilder(propertyConversionMethod)
    filter.block()
    return filter.params
}

/**
 * Represents a filter operation for a column using a specific operator and a value.
 */
data class FilterOperation(val column: String, val operator: FilterOperator, val value: String)

/**
 * Represents a single filter operation
 */
enum class FilterOperator(val identifier: String) {
    EQ("eq"),
    NEQ("neq"),
    GT("gt"),
    GTE("gte"),
    LT("lt"),
    LTE("lte"),
    LIKE("like"),
    MATCH("match"),
    ILIKE("ilike"),
    IMATCH("imatch"),
    IS("is"),
    IN("in"),
    CS("cs"),
    CD("cd"),
    SL("sl"),
    SR("sr"),
    NXL("nxl"),
    NXR("nxr"),
    ADJ("adj"),
    OV("ov"),
    FTS("fts"),
    PLFTS("plfts"),
    PHFTS("phfts"),
    WFTS("wfts"),
}

/**
 * Used to search rows using a full text search. See [Postgrest](https://postgrest.org/en/stable/api.html#full-text-search) for more information
 */
enum class TextSearchType(val identifier: String) {
    TSVECTOR("tsvector"),
    PLAINTO("plainto"),
    PHRASETO("phraseto"),
    WEBSEARCH("websearch")
}