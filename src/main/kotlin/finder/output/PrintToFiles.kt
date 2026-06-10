package finder.output

import finder.DuplicateFinderOptions
import finder.DuplicateFinderReport
import finder.indexing.Chunk
import finder.similarity.similarity
import java.nio.file.*
import kotlin.io.path.writeText

fun printToFiles(
    report: DuplicateFinderReport,
    options: DuplicateFinderOptions
) {
    Files.createDirectories(options.outputDirectory)

    report.duplicates.toList()
        .distinctBy { it.first.content }
        .forEach { (referenceChunk, duplicateChunks) ->
            val fileName =
                "${referenceChunk.path.replace("/", "-").replace("\\", "")}_" +
                        "${referenceChunk.coordinates}_" +
                        "${duplicateChunks.size}.txt"

            val outputFile = options.outputDirectory.resolve(fileName)

            val content = buildString {
                referenceChunkHeader()
                referenceChunkInfo(referenceChunk)
                referenceChunkContent(referenceChunk)
                separator()
                duplicateChunksHeader()
                duplicateChunksInfo(referenceChunk, duplicateChunks, options)
            }

            outputFile.writeText(content)
        }
}

private fun StringBuilder.referenceChunkHeader() = appendLine("Reference chunk:").appendBlankLine()
private fun StringBuilder.referenceChunkInfo(referenceChunk: Chunk) = appendLine("${referenceChunk.path} ${referenceChunk.coordinates}").appendBlankLine()
private fun StringBuilder.referenceChunkContent(referenceChunk: Chunk) = appendLine(referenceChunk.content.trim()).appendBlankLine()
private fun StringBuilder.separator() = appendLine("====================").appendBlankLine()
private fun StringBuilder.duplicateChunksHeader() = appendLine("Duplicate chunks:").appendBlankLine()
private fun StringBuilder.appendBlankLine() = appendLine("\n")
private fun StringBuilder.duplicateChunksInfo(rChunk: Chunk, dChunks: List<Chunk>, opts: DuplicateFinderOptions) =
    dChunks.forEach { dChunk ->
        append(
            "${dChunk.similarity(rChunk, opts)}% " +
                    "${dChunk.path} " +
                    "${dChunk.coordinates}\n"
        )
    }