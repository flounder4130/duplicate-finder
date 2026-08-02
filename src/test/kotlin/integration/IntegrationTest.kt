package integration

import finder.main
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.nio.file.*
import kotlin.test.*

class IntegrationTest {

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testDataDir: Path

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testOutputDir: Path

    @BeforeTest
    fun setup() {
        generateTestData(outputDir = testDataDir)
        injectDuplicates(testDataDir)
    }

    @Test
    fun test() {
        main(
            args = arrayOf(
                "-r=$testDataDir",
                "-f=*:line",
                "-o=$testOutputDir",
                "-s=0.8",
                "-l=50",
                "-v",
                "-w",
                "-ui=none"
            )
        )

        val lines = Files.list(testOutputDir).use { reports ->
            reports.flatMap { Files.lines(it) }.toList()
        }

        assert(fuzzyMatches.all { it in lines })
        assert(EXACT_MATCH in lines)
    }
}