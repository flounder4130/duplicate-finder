package finder.indexing

import finder.*
import finder.parsing.parser
import java.nio.file.Path
import kotlin.io.path.readText

class FileProcessor(val options: DuplicateFinderOptions) {

    fun fileToChunks(path: Path): List<Chunk> {
        val content = path.readText()
        return contentToChunks(content, path)
    }

    fun contentToChunks(content: String, path: Path): List<Chunk> {
        return try {
            val pathFromRoot = options.root.relativize(path).toString()
            val normalize = !options.keepWhitespace
            parser(options, path).parse(content, pathFromRoot)
                .onEach { if (normalize) it.content = normalizeWhitespace(it.content) }
                .filter { it.content.length >= options.minLength }
        } catch (e: Exception) {
            if (options.verbose) System.err.println("Error parsing file: $path ${e.javaClass.name}")
            emptyList()
        }
    }

    private fun normalizeWhitespace(content: String) = content.replace(Regex("\\s+"), " ").trim()
}
