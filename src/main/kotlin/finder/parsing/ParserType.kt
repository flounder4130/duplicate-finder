package finder.parsing

@Suppress("SpellCheckingInspection")
enum class ParserType(val cliName: String) {
    FILE("file"),
    LINE("line"),
    MARKDOWN("md"),
    XML("xml"),
    ASCIIDOC("adoc"),
    PROPERTIES("properties");

    companion object {
        fun byName(name: String): ParserType? = entries.firstOrNull { it.cliName == name }
    }
}