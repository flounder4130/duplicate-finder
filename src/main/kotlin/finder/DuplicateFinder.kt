package finder

import finder.output.printToFiles
import finder.indexing.Index
import finder.parsing.ParserType
import finder.ui.swing.SwingUi
import finder.ui.compose.composeUi
import java.nio.file.*
import kotlin.collections.*
import org.apache.commons.cli.*
import java.io.PrintWriter
import kotlin.system.exitProcess
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

private const val DOCUMENTATION_URL = "https://flounder.dev/duplicate-finder/"

fun indexAndFind(options: DuplicateFinderOptions): DuplicateFinderReport {
    val index = Index.getInstance(options)
    val indexDuration = measureTime { index.indexDirectory(); index.computeDocFrequencies() }
    val (duplicates, findDuration) = measureTimedValue { findAll(options) }

    return DuplicateFinderReport(
        duplicates,
        indexDuration,
        findDuration
    )
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        runFromConfig(Path.of(".").toAbsolutePath().normalize())
        return
    }

    val options = Options().apply {
        listOf(
            Option("r", "root", true, "content root path (uses current directory if not specified)"),
            Option("p", "parser", true, "parser (line, file, xml, md, adoc, properties, auto), default: auto"),
            Option("o", "output", true, "output file path"),
            Option("v", "verbose", false, "print verbose output"),
            Option("s", "minSimilarity", true, "minimum similarity"),
            Option("l", "minLength", true, "minimum length"),
            Option("d", "minDuplicates", true, "minimum duplicates"),
            Option("f", "fileMask", true, "file mask"),
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

        val hasConfigOverrides = cmd.hasOption("parser") || cmd.hasOption("minSimilarity") ||
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
            "parser" to "auto",
            "gram" to "3",
            "ui" to "compose",
        )

        fun cmdOrDefault(name: String) = cmd.getOptionValue(name) ?: defaults[name] ?: error("No default")

        val outputPath = Path.of(cmdOrDefault("output"))
        val minSimilarity = cmdOrDefault("minSimilarity").toDouble()
        val minLength = cmdOrDefault("minLength").toInt()
        val minDuplicates = cmdOrDefault("minDuplicates").toInt()
        val fileMask = cmdOrDefault("fileMask").split(",").filter { it.isNotEmpty() }.toSet()
        val verbose = cmd.hasOption("verbose")
        val ui = cmdOrDefault("ui")
        val cacheNgrams = cmd.hasOption("cache")
        val keepWhitespace = cmd.hasOption("keepWhitespace")
        val parserOption = cmdOrDefault("parser")
        val ngramLength = cmdOrDefault("gram").toInt()
        val inlineNested = cmd.hasOption("inline")

        val availableParsers = setOf("line", "file", "xml", "md", "adoc", "properties", "auto")
        require(parserOption in availableParsers) {
            "Invalid parser: $parserOption. Allowed values are: $availableParsers"
        }
        val parser = when (parserOption) {
            "line" -> ParserType.LINE
            "file" -> ParserType.FILE
            "xml" -> ParserType.XML
            "md" -> ParserType.MARKDOWN
            "adoc" -> ParserType.ASCIIDOC
            "properties" -> ParserType.PROPERTIES
            "auto" -> ParserType.AUTO
            else -> {
                System.err.println("Unsupported parser: $parserOption, defaulting to 'auto'")
                ParserType.AUTO
            }
        }

        val finderOptions = DuplicateFinderOptions(
            root,
            minSimilarity,
            minLength.coerceAtLeast(ngramLength),
            minDuplicates,
            fileMask,
            parser,
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
    val report = indexAndFind(options)
    if (options.verbose) {
        println("Indexing took: ${report.indexDuration}")
        println("Analysis took: ${report.analysisDuration}")
    }
    printToFiles(report, options)
    when (ui) {
        "swing" -> SwingUi(report, options).show()
        "compose" -> composeUi(report, options)
    }
}
