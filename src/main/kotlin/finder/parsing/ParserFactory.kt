package finder.parsing

import finder.DuplicateFinderOptions
import finder.parsing.ParserType.*
import java.nio.file.Path

fun parser(options: DuplicateFinderOptions, path: Path): ContentParser = when (options.parserFor(path)) {
    FILE        -> FileParser()
    LINE        -> LineParser()
    MARKDOWN    -> MarkdownParser()
    XML         -> XmlParser(options.inlineNested)
    ASCIIDOC    -> AsciiDocParser()
    PROPERTIES  -> JavaPropertiesParser()
}
