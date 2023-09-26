package io.github.jan.supabase.postgrest.request

import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Returning
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteRequestTest {

    private lateinit var sut: PostgrestRequest

    @Test
    fun testCreateDeleteRequest_thenReturnCorrectValue() {
        sut = DeleteRequest(
            returning = Returning.REPRESENTATION,
            count = Count.EXACT,
            filter = mapOf("Key1" to listOf("Value1")),
            schema = "table"
        )

        assertEquals("DELETE", sut.method.value)
        assertEquals(listOf("return=representation", "count=exact"), sut.prefer)
        assertEquals("table", sut.schema)
        assertEquals(mapOf("Key1" to listOf("Value1")), sut.filter)
    }

    @Test
    fun testCreateDeleteRequest_withoutCount_thenReturnCorrectValue() {
        sut = DeleteRequest(
            returning = Returning.REPRESENTATION,
            count = null,
            filter = mapOf("Key1" to listOf("Value1")),
            schema = "table"
        )

        assertEquals("DELETE", sut.method.value)
        assertEquals(listOf("return=representation"), sut.prefer)
        assertEquals("table", sut.schema)
        assertEquals(mapOf("Key1" to listOf("Value1")), sut.filter)
    }

}