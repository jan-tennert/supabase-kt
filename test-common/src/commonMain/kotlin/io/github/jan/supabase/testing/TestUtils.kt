package io.github.jan.supabase.testing

import io.ktor.http.HttpMethod
import kotlin.test.assertEquals

const val TEST_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

fun assertMethodIs(expected: HttpMethod, actual: HttpMethod) {
    assertEquals(expected, actual, "Method should be $expected")
}

fun assertPathIs(expected: String, actual: String) {
    assertEquals(expected, actual, "Path should be '$expected'")
}