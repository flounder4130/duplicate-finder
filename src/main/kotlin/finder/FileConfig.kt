package finder

import finder.parsing.*
import java.nio.file.*
import kotlin.io.path.*

data class FileConfig(
    val minSimilarity: Double = 0.9,
    val minLength: Int = 100,
    val minDuplicates: Int = 1,
    val fileMask: String = "",
    val ngramLength: Int = 3,
    val output: String = "./duplicate_finder_output",
    val ui: String = "compose",
    val verbose: Boolean = false,
    val cacheNgrams: Boolean = false,
    val keepWhitespace: Boolean = false,
    val inlineNested: Boolean = false,
) {
    fun toOptions(root: Path, outputOverride: Path?): DuplicateFinderOptions {
        return DuplicateFinderOptions(
            root = root,
            minSimilarity = minSimilarity,
            minLength = minLength.coerceAtLeast(ngramLength),
            minDuplicates = minDuplicates,
            fileMask = FileMask.resolve(fileMask),
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

        fun findConfigPath(root: Path): Path? =
            root.resolve(CONFIG_FILE_NAME).takeIf { it.exists() && Files.isRegularFile(it) }

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
                minSimilarity = props["minSimilarity"]?.toDoubleOrNull() ?: defaults.minSimilarity,
                minLength = props["minLength"]?.toIntOrNull() ?: defaults.minLength,
                minDuplicates = props["minDuplicates"]?.toIntOrNull() ?: defaults.minDuplicates,
                fileMask = props["fileMask"] ?: defaults.fileMask,
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
