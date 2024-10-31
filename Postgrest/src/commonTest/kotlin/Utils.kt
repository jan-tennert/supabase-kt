import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder

@SupabaseInternal
inline fun postgrestRequest(
    config: Postgrest.Config = Postgrest.Config(),
    block: PostgrestRequestBuilder.() -> Unit
): PostgrestRequestBuilder {
    val filter = PostgrestRequestBuilder(config)
    filter.block()
    return filter
}