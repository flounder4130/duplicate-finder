package finder.parsing

import finder.DuplicateFinderOptions
import finder.parsing.ParserType.*
import java.nio.file.Path

fun parser(options: DuplicateFinderOptions, path: Path): ContentParser = when (options.parserFor(path)) {
    FILE        -> FileParser(options)
    LINE        -> LineParser(options)
    MARKDOWN    -> MarkdownParser(options)
    XML         -> XmlParser(options)
    ASCIIDOC    -> AsciiDocParser(options)
    PROPERTIES  -> JavaPropertiesParser(options)
}
