package finder

import finder.parsing.ParserType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

/**
 * Optional run configuration loaded from a simple newline-separated
 * `property=value` file ([CONFIG_FILE_NAME]) in the content root. Blank lines and
 * lines starting with `#` are ignored; unknown keys are ignored. Anything not set
 * falls back to the defaults below (the same defaults the CLI uses).
 *
 * Example `duplicate-finder.properties`:
 * ```
 * parser=xml
 * fileMask=topic
 * minSimilarity=0.9
 * minLength=100
 * inline=true
 * ```
 */
data class FileConfig(
    val parser: String = "auto",
    val minSimilarity: Double = 0.9,
    val minLength: Int = 100,
    val minDuplicates: Int = 1,
    val fileMask: Set<String> = emptySet(),
    val ngramLength: Int = 3,
    val output: String = "./duplicate_finder_output",
    val ui: String = "compose",
    val verbose: Boolean = false,
    val cacheNgrams: Boolean = false,
    val keepWhitespace: Boolean = false,
    val inlineNested: Boolean = false,
) {
    fun toOptions(root: Path, outputOverride: Path?): DuplicateFinderOptions {
        val parserType = when (parser) {
            "line" -> ParserType.LINE
            "file" -> ParserType.FILE
            "xml" -> ParserType.XML
            "md" -> ParserType.MARKDOWN
            "adoc" -> ParserType.ASCIIDOC
            "properties" -> ParserType.PROPERTIES
            "auto" -> ParserType.AUTO
            else -> {
                System.err.println("Unsupported parser: $parser, defaulting to 'auto'")
                ParserType.AUTO
            }
        }
        return DuplicateFinderOptions(
            root = root,
            minSimilarity = minSimilarity,
            minLength = minLength.coerceAtLeast(ngramLength),
            minDuplicates = minDuplicates,
            fileMask = fileMask,
            parserType = parserType,
            verbose = verbose,
            cacheNgrams = cacheNgrams,
            ngramLength = ngramLength,
            outputDirectory = outputOverride ?: Path.of(output),
            keepWhitespace = keepWhitespace,
            inlineNested = inlineNested,
        )
    }

    companion object {
        const val CONFIG_FILE_NAME = "duplicate-finder.properties"

        /** The config file in [root], or null if it doesn't exist. */
        fun findConfigPath(root: Path): Path? =
            root.resolve(CONFIG_FILE_NAME).takeIf { it.exists() && Files.isRegularFile(it) }

        /** Load and parse the config from [root], or null if there is none. */
        fun load(root: Path): FileConfig? {
            val path = findConfigPath(root) ?: return null
            val props = HashMap<String, String>()
            path.readLines().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEach
                val eq = line.indexOf('=')
                if (eq <= 0) return@forEach
                props[line.substring(0, eq).trim()] = line.substring(eq + 1).trim()
            }
            val defaults = FileConfig()
            return FileConfig(
                parser = props["parser"] ?: defaults.parser,
                minSimilarity = props["minSimilarity"]?.toDoubleOrNull() ?: defaults.minSimilarity,
                minLength = props["minLength"]?.toIntOrNull() ?: defaults.minLength,
                minDuplicates = props["minDuplicates"]?.toIntOrNull() ?: defaults.minDuplicates,
                fileMask = props["fileMask"]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
                    ?: defaults.fileMask,
                ngramLength = props["gram"]?.toIntOrNull() ?: defaults.ngramLength,
                output = props["output"] ?: defaults.output,
                ui = props["ui"] ?: defaults.ui,
                verbose = props["verbose"]?.toBooleanStrictOrNull() ?: defaults.verbose,
                cacheNgrams = props["cache"]?.toBooleanStrictOrNull() ?: defaults.cacheNgrams,
                keepWhitespace = props["keepWhitespace"]?.toBooleanStrictOrNull() ?: defaults.keepWhitespace,
                inlineNested = props["inline"]?.toBooleanStrictOrNull() ?: defaults.inlineNested,
            )
        }
    }
}
