package io.github.jan.supabase.postgrest.request

import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Returning
import kotlinx.serialization.json.JsonArray
import kotlin.test.Test
import kotlin.test.assertEquals

class InsertRequestTest {

    private lateinit var sut: PostgrestRequest

    @Test
    fun testCreateInsertRequest_withUpsert_thenReturnCorrectValue() {
        sut = InsertRequest(
            onConflict = "on_conflict",
            returning = Returning.REPRESENTATION,
            count = Count.EXACT,
            upsert = true,
            body = JsonArray(listOf()),
            filter = mapOf("Key1" to listOf("Value1")),
            schema = "table"
        )

        assertEquals("POST", sut.method.value)
        assertEquals(
            listOf(
                "return=representation",
                "resolution=merge-duplicates", "count=exact"
            ), sut.prefer
        )
        assertEquals("table", sut.schema)
        assertEquals(mapOf("Key1" to listOf("Value1")), sut.filter)
        assertEquals(JsonArray(listOf()), sut.body)
        assertEquals(mapOf("on_conflict" to "on_conflict"), sut.urlParams)
    }

    @Test
    fun testCreateInsertRequest_notUpsert_thenReturnCorrectValue() {
        sut = InsertRequest(
            onConflict = "on_conflict",
            returning = Returning.REPRESENTATION,
            count = Count.EXACT,
            upsert = false,
            body = JsonArray(listOf()),
            filter = mapOf("Key1" to listOf("Value1")),
            schema = "table"
        )

        assertEquals("POST", sut.method.value)
        assertEquals(
            listOf(
                "return=representation",
                "count=exact"
            ), sut.prefer
        )
        assertEquals("table", sut.schema)
        assertEquals(mapOf("Key1" to listOf("Value1")), sut.filter)
        assertEquals(JsonArray(listOf()), sut.body)
        assertEquals(emptyMap(), sut.urlParams)
    }

    @Test
    fun testCreateInsertRequest_withoutCount_thenReturnCorrectValue() {
        sut = InsertRequest(
            onConflict = "on_conflict",
            returning = Returning.REPRESENTATION,
            count = null,
            upsert = false,
            body = JsonArray(listOf()),
            filter = mapOf("Key1" to listOf("Value1")),
            schema = "table"
        )

        assertEquals("POST", sut.method.value)
        assertEquals(
            listOf(
                "return=representation",
            ), sut.prefer
        )
        assertEquals("table", sut.schema)
        assertEquals(mapOf("Key1" to listOf("Value1")), sut.filter)
        assertEquals(JsonArray(listOf()), sut.body)
        assertEquals(emptyMap(), sut.urlParams)
    }

}