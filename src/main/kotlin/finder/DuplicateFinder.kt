package finder

import finder.output.printToFiles
import finder.indexing.Index
import finder.parsing.*
import finder.ui.swing.SwingUi
import finder.ui.compose.composeUi
import java.nio.file.*
import kotlin.collections.*
import org.apache.commons.cli.*
import java.io.PrintWriter
import kotlin.system.exitProcess
import kotlin.time.*

private const val DOCUMENTATION_URL = "https://flounder.dev/duplicate-finder/"

class DuplicateFinder(private val options: DuplicateFinderOptions) {

    val index = Index(options)

    fun run(): DuplicateFinderReport {
        val indexDuration = measureTime { index.indexDirectory(); index.computeDocFrequencies() }
        val (duplicates, findDuration) = measureTimedValue { findAll(index) }

        return DuplicateFinderReport(
            duplicates,
            indexDuration,
            findDuration
        )
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        runFromConfig(Path.of(".").toAbsolutePath().normalize())
        return
    }

    val options = Options().apply {
        listOf(
            Option("r", "root", true, "content root path (uses current directory if not specified)"),
            Option("o", "output", true, "output file path"),
            Option("v", "verbose", false, "print verbose output"),
            Option("s", "minSimilarity", true, "minimum similarity"),
            Option("l", "minLength", true, "minimum length"),
            Option("d", "minDuplicates", true, "minimum duplicates"),
            Option("f", "fileMask", true, "file mask: comma-separated extension:parser; for example 'xml,md', 'topic:xml,md:md'; '*' matches any extension, for example '*:line'. Default: ${FileMask.defaultsDescription}"),
            Option("ui", "ui", true, "UI to use (swing, compose, none), default: compose"),
            Option("c", "cache", false, "cache trigrams (more memory; usually slower)"),
            Option("g", "gram", false, "ngram length"),
            Option("w", "keepWhitespace", false, "parse without normalizing whitespace"),
            Option("i", "inline", false, "inline nested content in the enclosing elements"),
        ).forEach(::addOption)
    }

    try {
        val cmd = DefaultParser().parse(options, args)

        val root = if (cmd.hasOption("root")) {
            Path.of(cmd.getOptionValue("root"))
        } else {
            Path.of(".").toAbsolutePath().normalize()
        }

        val hasConfigOverrides = cmd.hasOption("minSimilarity") ||
                cmd.hasOption("minLength") || cmd.hasOption("minDuplicates") ||
                cmd.hasOption("fileMask") || cmd.hasOption("gram") ||
                cmd.hasOption("keepWhitespace") || cmd.hasOption("inline")

        if (!hasConfigOverrides) {
            val configPath = FileConfig.findConfigPath(root)
            if (configPath != null) {
                runFromConfig(root, cmd.getOptionValue("output"), cmd.getOptionValue("ui"), cmd.hasOption("verbose"))
                return
            }
        }

        val defaults = mapOf(
            "output" to "./duplicate_finder_output",
            "minSimilarity" to "0.9",
            "minLength" to "100",
            "minDuplicates" to "1",
            "fileMask" to "",
            "gram" to "3",
            "ui" to "compose",
        )

        fun cmdOrDefault(name: String) = cmd.getOptionValue(name) ?: defaults[name] ?: error("No default")

        val outputPath = Path.of(cmdOrDefault("output"))
        val minSimilarity = cmdOrDefault("minSimilarity").toDouble()
        val minLength = cmdOrDefault("minLength").toInt()
        val minDuplicates = cmdOrDefault("minDuplicates").toInt()
        val verbose = cmd.hasOption("verbose")
        val ui = cmdOrDefault("ui")
        val cacheNgrams = cmd.hasOption("cache")
        val keepWhitespace = cmd.hasOption("keepWhitespace")
        val ngramLength = cmdOrDefault("gram").toInt()
        val inlineNested = cmd.hasOption("inline")
        val fileMask = FileMask.resolve(cmdOrDefault("fileMask"))

        val finderOptions = DuplicateFinderOptions(
            root,
            minSimilarity,
            minLength.coerceAtLeast(ngramLength),
            minDuplicates,
            fileMask,
            verbose,
            cacheNgrams,
            ngramLength,
            outputPath,
            keepWhitespace,
            inlineNested
        )

        runWithOptions(finderOptions, ui)

    } catch (e: ParseException) {
        println("\n${e.message}\n")
        val formatter = HelpFormatter()
        formatter.printOptions(PrintWriter(System.out, true), 120, options, 0, 5)
        println("\nFor more information, see $DOCUMENTATION_URL\n")
        exitProcess(1)
    }
}

private fun runFromConfig(
    root: Path,
    outputOverride: String? = null,
    uiOverride: String? = null,
    verboseOverride: Boolean = false
) {
    val config = FileConfig.load(root) ?: run {
        System.err.println("""
            No ${FileConfig.CONFIG_FILE_NAME} found in ${root.toAbsolutePath()}, using defaults.
            You can specify settings via ${FileConfig.CONFIG_FILE_NAME} or CLI arguments.
            For more information, see $DOCUMENTATION_URL
            Analyzing files in ${root.toAbsolutePath()}
        """.trimIndent()
        )
        FileConfig()
    }

    val outputPath = outputOverride?.let { Path.of(it) }
    val options = config.toOptions(root, outputPath)
    val ui = uiOverride ?: config.ui
    val finalOptions = if (verboseOverride) options.copy(verbose = true) else options

    runWithOptions(finalOptions, ui)
}

private fun runWithOptions(options: DuplicateFinderOptions, ui: String) {
    val finder = DuplicateFinder(options)
    val report = finder.run()
    if (options.verbose) {
        println("Indexing took: ${report.indexDuration}")
        println("Analysis took: ${report.analysisDuration}")
    }
    printToFiles(report, options)
    when (ui) {
        "swing" -> SwingUi(report, options, finder.index).show()
        "compose" -> composeUi(report, options, finder.index)
    }
}
