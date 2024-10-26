package io.github.jan.supabase.postgrest.annotations

import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder

/**
 * Marks a class as selectable.
 *
 * When using the **ksp-compiler** this annotation will be processed and columns for [PostgrestQueryBuilder.select] will be generated.
 *
 * The columns can be accessed via the generated extension property for the companion object of the class.
 *
 * **Note:**
 * - All classes marked with this annotation must have a companion object, which can be empty, and must be a data class
 * - All parameters in the primary constructor must a primitive type`*` or a type that is also marked with [Selectable]
 * - Parameters may be marked with [ColumnName], [ApplyFunction], [Cast], [JsonPath], [Foreign].
 *
 * `*` Available primitive types are: String, Int, Long, Float, Double, Boolean
 *
 * Example usage:
 * ```kotlin
 * @Selectable
 * data class User(
 *    val id: Int,
 *    val name: String,
 *    val age: Int
 *    //...
 * ) {
 *     companion object
 * }
 *
 * //Usage
 * val users: List<User> = supabase.from("users").select(User.columns).decodeList()
 * ```
 * @see ApplyFunction
 * @see Cast
 * @see ColumnName
 * @see Foreign
 * @see JsonPath
 * @see PostgrestQueryBuilder.select
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Selectable
