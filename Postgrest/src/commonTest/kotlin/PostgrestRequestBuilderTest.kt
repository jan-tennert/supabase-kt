import io.github.jan.supabase.postgrest.MapColumnRegistry
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.Returning
import io.github.jan.supabase.postgrest.query.select
import io.ktor.http.HttpHeaders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PostgrestRequestBuilderTest {

    @Test
    fun testCount() {
        val request = postgrestRequest {
            count(Count.ESTIMATED)
        }
        assertEquals(Count.ESTIMATED, request.count)
    }

    @Test
    fun testSelect() {
        val request = postgrestRequest {
            select()
        }
        assertEquals(Returning.Representation(), request.returning)
    }

    @Test
    fun testSelectWithSelectable() {
        val columnRegistry = MapColumnRegistry()
        columnRegistry.registerColumns("TestType", "column1, column2")
        val request = postgrestRequest(
            config = Postgrest.Config(columnRegistry = columnRegistry)
        ) {
            select<TestType>()
        }
        assertIs<Returning.Representation>(request.returning)
        assertEquals("column1,column2", (request.returning as Returning.Representation).columns.value)
    }

    @Test
    fun testSingle() {
        val request = postgrestRequest {
            single()
        }
        assertEquals("application/vnd.pgrst.object+json", request.headers[HttpHeaders.Accept])
    }

    @Test
    fun testOrderWithoutReferencedTable() {
        val request = postgrestRequest {
            order("messages", Order.ASCENDING, true)
        }
        assertEquals(listOf("messages.asc.nullsfirst"), request.params["order"])
    }

    @Test
    fun testOrderWithoutReferencedTableNullsLast() {
        val request = postgrestRequest {
            order("messages", Order.DESCENDING, false)
        }
        assertEquals(listOf("messages.desc.nullslast"), request.params["order"])
    }

    @Test
    fun testOrderWithReferencedTable() {
        val request = postgrestRequest {
            order("messages", Order.ASCENDING, true, "table")
        }
        assertEquals(listOf("messages.asc.nullsfirst"), request.params["table.order"])
    }

    @Test
    fun testLimitWithoutReferencedTable() {
        val request = postgrestRequest {
            limit(10)
        }
        assertEquals(listOf("10"), request.params["limit"])
    }

    @Test
    fun testLimitWithReferencedTable() {
        val request = postgrestRequest {
            limit(10, "table")
        }
        assertEquals(listOf("10"), request.params["table.limit"])
    }

    @Test
    fun testRangeWithoutReferencedTable() {
        val request = postgrestRequest {
            range(10, 20)
        }
        assertEquals(listOf("10"), request.params["offset"])
        assertEquals(listOf("11"), request.params["limit"])
    }

    @Test
    fun testRangeWithReferencedTable() {
        val request = postgrestRequest {
            range(10, 20, "table")
        }
        assertEquals(listOf("10"), request.params["table.offset"])
        assertEquals(listOf("11"), request.params["table.limit"])
    }

    @Test
    fun testGeojson() {
        val request = postgrestRequest {
            geojson()
        }
        assertEquals("application/geo+json", request.headers[HttpHeaders.Accept])
    }

    @Test
    fun testCsv() {
        val request = postgrestRequest {
            csv()
        }
        assertEquals("text/csv", request.headers[HttpHeaders.Accept])
    }

    @Test
    fun testExplainWithNoOptions() {
        val request = postgrestRequest {
            explain()
        }
        assertEquals("application/vnd.pgrst.plan+text; for=\"application/json\"; options=;", request.headers[HttpHeaders.Accept])
    }

    @Test
    fun testExplainWithOptions() {
        val request = postgrestRequest {
            explain(
                analyze = true,
                verbose = true,
                buffers = true,
                settings = true,
                wal = true,
                format = "json"
            )
        }
        assertEquals("application/vnd.pgrst.plan+json; for=\"application/json\"; options=analyze|verbose|settings|buffers|wal;", request.headers[HttpHeaders.Accept])
    }

}
