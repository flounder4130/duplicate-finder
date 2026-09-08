package finder.output

import finder.model.DuplicateFinderReport
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

fun printToFiles(report: DuplicateFinderReport, outputDirectory: Path) {
    Files.createDirectories(outputDirectory)
    report.duplicates.entries.distinctBy { it.key.content }.forEach { (reference, duplicates) ->
        val name = reference.location.path.toString().replace("/", "-").replace("\\", "")
        val file = outputDirectory.resolve("${name}_${reference.location.line}_${duplicates.size}.txt")
        file.writeText(buildString {
            appendLine("Reference chunk:").appendLine().appendLine()
            appendLine("${reference.location.path} ${reference.location.line}").appendLine().appendLine()
            appendLine(reference.content.trim()).appendLine().appendLine()
            appendLine("====================").appendLine().appendLine()
            appendLine("Duplicate chunks:").appendLine().appendLine()
            duplicates.forEach { match ->
                val location = match.chunk.location
                appendLine("${(match.similarity * 100).toInt()}% ${location.path} ${location.line}")
            }
        })
    }
}