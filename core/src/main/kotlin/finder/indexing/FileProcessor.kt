package finder.indexing

import finder.DuplicateFinderOptions
import finder.FileIndexingException
import finder.model.TextRange
import finder.parsing.parser
import java.nio.file.Path
import kotlin.io.path.readText

private val WHITESPACE_REGEX = Regex("\\s+")

internal fun normalizeWhitespace(content: String): String = content.replace(WHITESPACE_REGEX, " ").trim()

internal class FileProcessor(val options: DuplicateFinderOptions) {
    fun fileToChunks(path: Path): List<Chunk> = try {
        contentToChunks(path.readText(), path)
    } catch (e: FileIndexingException) {
        throw e
    } catch (e: Exception) {
        throw FileIndexingException(path, e)
    }

    fun contentToChunks(content: String, path: Path): List<Chunk> = try {
        val relative = options.root.relativize(path).toString()
        val chunks = parser(options, path).parse(content, relative)
        val xmlOffsets = if (chunks.any { it is XmlChunk }) originalXmlOffsets(content) else null
        chunks.onEach { chunk ->
            val coordinates = chunk.coordinates
            chunk.sourceRange = when {
                chunk is FileChunk -> TextRange(0, content.length)
                chunk is XmlChunk && coordinates is OffsetCoordinates && xmlOffsets != null ->
                    TextRange(xmlOffsets[coordinates.start], xmlOffsets[coordinates.end])
                else -> chunk.sourceRange
            }
            if (!options.keepWhitespace) chunk.content = normalizeWhitespace(chunk.content)
        }.filter { it.content.length >= options.minLength }
    } catch (e: Exception) {
        throw FileIndexingException(path, e)
    }

    // XML offsets are measured after BOM removal and CRLF/CR normalization.
    private fun originalXmlOffsets(content: String): IntArray {
        val offsets = IntArray(content.length + 1)
        var source = if (content.startsWith('\uFEFF')) 1 else 0
        var normalized = 0
        while (source < content.length) {
            offsets[normalized++] = source
            source += if (content[source] == '\r' && content.getOrNull(source + 1) == '\n') 2 else 1
        }
        offsets[normalized] = source
        return offsets
    }
}