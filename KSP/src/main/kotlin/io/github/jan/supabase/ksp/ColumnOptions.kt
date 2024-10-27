package io.github.jan.supabase.ksp

data class ColumnOptions(
    val alias: String,
    val columnName: String?,
    val isForeign: Boolean,
    val jsonPath: List<String>?,
    val returnAsText: Boolean,
    val function: String?,
    val cast: String?,
    val innerColumns: String,
    val jsonKey: String?
)
