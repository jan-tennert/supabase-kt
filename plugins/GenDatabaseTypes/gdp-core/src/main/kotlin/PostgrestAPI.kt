data class PostgrestAPI(
    val definitions: Map<String, PostgrestDefinition>
)

data class PostgrestDefinition(
    val properties: Map<String, PostgrestProperty>,
    val required: List<String>,
    val type: String
)

data class PostgrestProperty(
    val format: String? = null,
    val type: String? = null,
    val items: List<PostgrestProperty> = emptyList()
)
