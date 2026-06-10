package integration

import finder.main
import org.junit.jupiter.api.Test
import java.nio.file.*
import kotlin.io.path.isDirectory
import kotlin.test.*

const val TEST_OUTPUT_DIR = "./test_out"

class IntegrationTest {

    @BeforeTest
    fun setup() {
        checkExistsAndEmpty(Path.of(TEST_DATA_DIR))
        checkExistsAndEmpty(Path.of(TEST_OUTPUT_DIR))
        generateTestData()
        injectDuplicates(TEST_DATA_DIR)
    }

    @Test
    fun test() {
        main(
            args = arrayOf(
                "-r=$TEST_DATA_DIR",
                "-f=*:line",
                "-o=$TEST_OUTPUT_DIR",
                "-s=0.8",
                "-l=50",
                "-v",
                "-w",
                "-ui=none"
            )
        )

        val lines = Files.list(Path.of(TEST_OUTPUT_DIR))
            .flatMap { Files.lines(it) }
            .toList()

        assert(fuzzyMatches.all { it in lines })
        assert(EXACT_MATCH in lines)
    }

    @AfterTest
    fun tearDown() {
        clearDirectory(Path.of(TEST_DATA_DIR))
        clearDirectory(Path.of(TEST_OUTPUT_DIR))
    }
}

private fun clearDirectory(path: Path) = Files.walk(path)
    .sorted(Comparator.reverseOrder())
    .forEach(Files::delete)

private fun Path.isEmptyDirectory() = isDirectory() && Files.list(this).toList().isEmpty()

private fun checkExistsAndEmpty(path: Path) {
    Files.createDirectories(path)
    if (!path.isEmptyDirectory()) {
        error("Directory not empty. Double-check and delete the contents manually: $path")
    }
}