package finder

import finder.output.printToFiles
import finder.parsing.ParserType
import finder.ui.compose.heatMapText
import finder.ui.sort.SortBy
import finder.ui.sort.chunkComparator
import finder.ui.swing.ListsData
import finder.ui.swing.heatMapDocument
import finder.ui.utils.filterClustered
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class ClientTest {
    @TempDir lateinit var root: Path

    private fun finder() = DuplicateFinder(DuplicateFinderOptions(
        root = root,
        fileMask = mapOf("txt" to ParserType.LINE),
        minLength = 20,
    )).apply {
        val content = "This shared passage demonstrates duplicate results and heat-map rendering in both clients."
        updateContent(Path.of("a.txt"), content)
        updateContent(Path.of("b.txt"), content)
        updateContent(Path.of("c.txt"), content.replace("shared", "common"))
    }

    @Test fun `exporter consumes immutable scored reports`() {
        val report = finder().report
        val directory = root.resolve("output")
        printToFiles(report, directory)
        val files = Files.list(directory).use { it.toList() }
        assertTrue(files.isNotEmpty())
        val output = files.joinToString("\n") { Files.readString(it) }
        assertContains(output, "Reference chunk:")
        assertContains(output, "Duplicate chunks:")
        assertContains(output, "100%")
        assertTrue(report.duplicates.keys.all { it.content in output })
    }

    @Test fun `both renderers consume the same numerical heat map`() {
        val finder = finder()
        val result = finder.findFuzzy(FuzzyQuery.Text(finder.report.duplicates.keys.first().content))
        val heatMap = finder.heatMap(result.referenceContent, result.matches)
        val swing = heatMapDocument(heatMap)
        val compose = heatMapText(heatMap)
        assertEquals(heatMap.content, swing.getText(0, swing.length))
        assertEquals(heatMap.content, compose.text)
        assertEquals(heatMap.content.length, compose.spanStyles.size)
        assertTrue(compose.spanStyles.all { it.end == it.start + 1 })
    }

    @Test fun `list sorting and clustering use public result identities and scores`() {
        val report = finder().report.duplicates
        val sorted = report.toList().sortedWith(chunkComparator(SortBy.MAX_AVG_SIMILARITY))
        val averages = sorted.map { it.second.map { match -> match.similarity }.average() }
        assertEquals(averages.sortedDescending(), averages)
        val clustered = report.filterClustered(true)
        assertEquals(1, clustered.size)
        val lists = ListsData(report)
        val reference = clustered.keys.single()
        lists.showDuplicatesFor(reference)
        assertEquals(report.getValue(reference).size, lists.duplicateChunksListModel.size)
        assertEquals(report.getValue(reference).first().chunk, lists.duplicateChunksListModel.firstElement().chunk)
        lists.sort(SortBy.MAX_AVG_SIMILARITY)
        assertEquals(1, lists.referenceChunksListModel.size)
        lists.showInClusters(false)
        assertEquals(report.size, lists.referenceChunksListModel.size)
    }
}