package example

import finder.*
import finder.parsing.ParserType
import java.nio.file.Path

fun main(args: Array<String>) {
    val finder = DuplicateFinder(DuplicateFinderOptions(
        root = Path.of(args.firstOrNull() ?: "."),
        fileMask = mapOf("topic" to ParserType.XML, "txt" to ParserType.LINE),
        minLength = 40,
        analysis = AnalysisOptions(minSimilarity = 0.90),
    ))

    if (args.isNotEmpty()) {
        val indexing = finder.indexDirectory(FileErrorPolicy.SKIP)
        indexing.failures.forEach { System.err.println("Skipped ${it.path}: ${it.message}") }
    } else {
        val text = "The duplicate finder searches indexed content and returns matching passages with similarity scores."
        finder.updateContent(Path.of("first.txt"), text)
        finder.updateContent(Path.of("second.txt"), text.replace("passages", "sections"))
        finder.updateContent(Path.of("third.txt"), text)
    }

    finder.computeDf()
    val report = finder.report
    println("Reference chunks with duplicates: ${report.duplicates.size}")

    val reference = report.duplicates.keys.firstOrNull() ?: return
    val duplicates = finder.findFuzzy(FuzzyQuery.Reference(reference))
    duplicates.matches.take(3).forEach {
        println("${(it.similarity * 100).toInt()}% ${it.chunk}")
    }
    val heatMap = finder.heatMap(reference, duplicates.matches)
    println("Heat-map scores: ${heatMap.scores.size}")

    finder.removeFile(reference.location.path)
    println("After removal from the index: ${finder.report.duplicates.size} reference chunks")
}