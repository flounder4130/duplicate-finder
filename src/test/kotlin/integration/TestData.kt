package integration

import java.io.File
import java.nio.file.*
import kotlin.io.path.*

const val WORDS_FILE = "./src/test/resources/words"
const val NUM_TEST_FILES = 100
const val LINES_PER_FILE = 100
const val WORDS_PER_LINE = 20

const val EXACT_MATCH = "This is an exact match. It will be inserted in the test data as-is."
const val FUZZY_MATCH_TEMPLATE =
    "This is a fuzzy match. It will be inserted in the test data with minor changes."

val fuzzyMatches = listOf(
    "123 $FUZZY_MATCH_TEMPLATE",
    "234 $FUZZY_MATCH_TEMPLATE",
    "$FUZZY_MATCH_TEMPLATE 567",
    "$FUZZY_MATCH_TEMPLATE 789",
    FUZZY_MATCH_TEMPLATE.removeRange(5, 10),
    FUZZY_MATCH_TEMPLATE.removeRange(10, 15)
)

fun generateTestData(
    outputDir: Path,
    wordsFile: String = WORDS_FILE,
    numOutputFiles: Int = NUM_TEST_FILES,
    linesPerFile: Int = LINES_PER_FILE,
    wordsPerLine: Int = WORDS_PER_LINE,
) {
    val words = File(wordsFile).readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    repeat(numOutputFiles) { fileIndex ->
        val outputFile = outputDir.resolve("test_data_${fileIndex + 1}")
        val fileContent = StringBuilder().apply {
            repeat(linesPerFile) {
                val line = (1..wordsPerLine).joinToString(" ") { words.random() }
                append(line + "\n")
            }
        }
        outputFile.createFile()
        outputFile.writeText(fileContent.toString())
    }
}

fun injectDuplicates(testDataDir: Path) {
    Files.list(testDataDir).use { it.toList() }.apply {
        random().also { println("Exact match 1 inserted in file: ${it.fileName}") }.appendText("$EXACT_MATCH\n")
        random().also { println("Exact match 2 inserted in file: ${it.fileName}") }.appendText("$EXACT_MATCH\n")
        fuzzyMatches.forEach { random().appendText("$it\n") }
    }
}