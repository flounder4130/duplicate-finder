package finder.parsing

import finder.parsing.ParserType.*

object FileMask {
    val DEFAULT_PARSER = FILE

    const val WILDCARD = "*"

    @Suppress("SpellCheckingInspection")
    val defaultByExtension: Map<String, ParserType> = mapOf(
        "md" to MARKDOWN,
        "mdx" to MARKDOWN,
        "xml" to XML,
        "adoc" to ASCIIDOC,
        "asciidoc" to ASCIIDOC,
        "properties" to PROPERTIES,
    )

    fun forExtension(extension: String): ParserType = defaultByExtension[extension] ?: DEFAULT_PARSER

    val defaultsDescription: String
        get() = defaultByExtension.entries.joinToString(", ") { (ext, parser) -> "$ext:${parser.cliName}" }

    fun resolve(spec: String): Map<String, ParserType> {
        val entries = spec.split(",").map(String::trim).filter(String::isNotEmpty)
        if (entries.isEmpty()) return defaultByExtension
        return entries.mapNotNull { entry ->
            val parts = entry.split(":", limit = 2).map(String::trim)
            val extension = parts[0]
            if (extension.isEmpty()) {
                System.err.println("Ignoring fileMask entry with empty extension: '$entry'")
                return@mapNotNull null
            }
            val explicit = parts.getOrNull(1)?.takeIf(String::isNotEmpty)?.let { name ->
                ParserType.byName(name) ?: run {
                    System.err.println("Unsupported parser '$name' for extension '$extension', using default")
                    null
                }
            }
            extension to (explicit ?: forExtension(extension))
        }.toMap()
    }
}
