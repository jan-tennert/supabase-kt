import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.postgrest.ColumnRegistry
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder

@SupabaseInternal
inline fun postgrestRequest(
    propertyConversionMethod: PropertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE,
    columnRegistry: ColumnRegistry = ColumnRegistry(),
    block: PostgrestRequestBuilder.() -> Unit
): PostgrestRequestBuilder {
    val filter = PostgrestRequestBuilder(propertyConversionMethod, columnRegistry)
    filter.block()
    return filter
}