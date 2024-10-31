import io.github.jan.supabase.postgrest.MapColumnRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class ColumnRegistryTest {

    @Test
    fun testColumnRegistry() {
        val registry = MapColumnRegistry()
        registry.registerColumns("Test", "column1, column2")
        assertEquals("column1, column2", registry.getColumns(Test::class))
    }

}