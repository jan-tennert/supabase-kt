package io.github.jan.supabase.postgrest.annotations

import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.uuid.Uuid

/**
 * Annotates a class as selectable.
 *
 * When using the **ksp-compiler** this annotation will be processed and columns for [PostgrestQueryBuilder.select] will be generated.
 *
 * The columns can be accessed via the generated extension property for the companion object of the class.
 *
 * **Note:**
 * - All classes annotated with this annotation must have a companion object, which can be empty, and must be a data class
 * - All parameters in the primary constructor must a primitive type¹, a type that is also annotated with [Selectable] or a serializable type.
 * - Parameters may be annotated with [ColumnName], [ApplyFunction], [Cast], [JsonPath], [Foreign].
 *
 * ¹: Available primitive types are: [String], [Int], [Long], [Float], [Double], [Boolean], [Byte], [Short], [Char], [Instant], [LocalDateTime], [Uuid], [LocalTime], [LocalDate], [JsonElement], [JsonObject], [JsonArray], [JsonPrimitive]
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
@SupabaseExperimental
annotation class Selectable