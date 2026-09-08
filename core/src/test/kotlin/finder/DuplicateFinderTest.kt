package finder

import finder.model.*
import finder.parsing.ParserType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.*

class DuplicateFinderTest {
    @TempDir lateinit var root: Path

    private val body = "The duplicate finder searches indexed content and returns matching passages with similarity scores."

    private fun options(similarity: Double = 0.90) = DuplicateFinderOptions(
        root = root,
        fileMask = mapOf("txt" to ParserType.LINE, "topic" to ParserType.XML),
        minLength = 40,
        analysis = AnalysisOptions(similarity),
    )

    private fun populated(options: DuplicateFinderOptions = options()) = DuplicateFinder(options).apply {
        updateContent(Path.of("a.txt"), body)
        updateContent(Path.of("b.txt"), body)
    }

    @Test fun `reports are cached snapshots and DF remains manual`() {
        val finder = populated()
        assertNull(finder.dfVersion)
        val original = finder.report
        assertEquals(2, original.duplicates.size)
        assertSame(original, finder.report)
        val reference = original.duplicates.keys.first()
        finder.computeDf()
        assertEquals(finder.indexVersion, finder.dfVersion)
        assertSame(original, finder.report)

        finder.updateContent(Path.of("c.txt"), body)
        assertTrue(finder.dfVersion!! < finder.indexVersion)
        assertEquals(2, finder.findFuzzy(FuzzyQuery.Reference(reference)).matches.size)
        val updated = finder.report
        assertNotSame(original, updated)
        assertEquals(2, original.duplicates.size)
        assertEquals(3, updated.duplicates.size)
        finder.computeDf()
        assertSame(updated, finder.report)

        finder.removeFile(reference.location.path)
        assertFailsWith<IllegalArgumentException> { finder.findFuzzy(FuzzyQuery.Reference(reference)) }
        assertTrue(finder.heatMap(reference, original.duplicates.getValue(reference)).scores.all { it == 1f })
        val noChange = finder.removeFile(reference.location.path)
        assertFalse(noChange.changed)
        assertEquals(finder.indexVersion, noChange.indexVersion)
    }

    @Test fun `query options do not mutate default analysis`() {
        val finder = populated(options().copy(analysis = AnalysisOptions(0.90, minDuplicates = 3)))
        val default = finder.report
        assertTrue(default.duplicates.isEmpty())
        assertEquals(2, finder.analyze(AnalysisOptions(0.85)).duplicates.size)
        assertSame(default, finder.report)
        val reference = finder.chunksInFile(Path.of("a.txt")).single()
        assertEquals(1, finder.findFuzzy(FuzzyQuery.Reference(reference)).matches.size)
        val result = finder.findFuzzy(FuzzyQuery.Text("  $body  "), minSimilarity = 1.0)
        assertEquals(body, result.referenceContent)
        assertEquals(2, result.matches.size)
        assertEquals(finder.indexVersion, result.indexVersion)
        assertTrue(result.matches.all { it.similarity == 1.0 })
        assertTrue(finder.findFuzzy(FuzzyQuery.Text("ab")).matches.isEmpty())
        for (invalid in listOf(0.0, -1.0, 1.1, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { finder.findFuzzy(FuzzyQuery.Text(body), invalid) }
            assertFailsWith<IllegalArgumentException> { AnalysisOptions(invalid) }
        }
    }

    @Test fun `callers cannot mutate index configuration or result collections`() {
        val mask = mutableMapOf("txt" to ParserType.LINE)
        val finder = populated(options().copy(fileMask = mask))
        mask.clear()
        finder.updateContent(Path.of("c.txt"), body)
        val report = finder.report
        assertFailsWith<UnsupportedOperationException> {
            (finder.options.fileMask as MutableMap).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (report.duplicates as MutableMap).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (report.duplicates.values.first() as MutableList).clear()
        }
        val result = finder.findFuzzy(FuzzyQuery.Text(body))
        assertFailsWith<UnsupportedOperationException> { (result.matches as MutableList).clear() }
        assertEquals(3, finder.report.duplicates.size)
    }

    @Test fun `failed updates and rebuilds preserve prior state`() {
        val finder = populated()
        finder.updateContent(Path.of("old.topic"), "<doc><p>$body</p></doc>")
        val reference = finder.chunksInFile(Path.of("old.topic")).single()
        val original = finder.report
        val version = finder.indexVersion
        val parseError = assertFailsWith<FileIndexingException> {
            finder.updateContent(Path.of("old.topic"), "<doc><p>broken</doc>")
        }
        assertEquals(root.resolve("old.topic"), parseError.path)
        assertFailsWith<FileIndexingException> { finder.updateFile(Path.of("missing.txt")) }
        assertEquals(version, finder.indexVersion)
        assertSame(original, finder.report)

        Files.writeString(root.resolve("bad.topic"), "<broken>")
        assertFailsWith<FileIndexingException> { finder.indexDirectory() }
        assertSame(original, finder.report)
        assertEquals(version, finder.indexVersion)
        assertEquals(2, finder.findFuzzy(FuzzyQuery.Reference(reference)).matches.size)

        Files.writeString(root.resolve("bad.topic"), "<doc><p>$body</p></doc>")
        val summary = finder.indexDirectory()
        assertEquals(1, summary.fileCount)
        assertEquals(1, summary.chunkCount)
        assertNull(finder.dfVersion)
        assertFailsWith<IllegalArgumentException> { finder.findFuzzy(FuzzyQuery.Reference(reference)) }
        Files.delete(root.resolve("bad.topic"))
        finder.indexDirectory()
        assertTrue(finder.report.duplicates.isEmpty())
        assertTrue(finder.chunksInFile(Path.of("bad.topic")).isEmpty())
    }

    @Test fun `paths and empty replacements have explicit semantics`() {
        val finder = DuplicateFinder(options())
        assertTrue(finder.report.duplicates.isEmpty())
        finder.updateContent(Path.of("sub/../a.txt"), body)
        val old = finder.chunksInFile(root.resolve("a.txt")).single()
        finder.updateContent(root.resolve("a.txt"), body)
        assertEquals(1, finder.chunksInFile(Path.of("a.txt")).size)
        assertFailsWith<IllegalArgumentException> { finder.findFuzzy(FuzzyQuery.Reference(old)) }
        assertFailsWith<IllegalArgumentException> { populated().findFuzzy(FuzzyQuery.Reference(old)) }
        assertFailsWith<IllegalArgumentException> { finder.updateContent(Path.of("../outside.txt"), body) }
        assertFailsWith<IllegalArgumentException> { finder.removeFile(root.parent.resolve("outside.txt")) }
        assertFailsWith<IllegalArgumentException> { finder.updateContent(Path.of("a.unknown"), body) }
        finder.updateContent(Path.of("a.txt"), "")
        assertTrue(finder.chunksInFile(Path.of("a.txt")).isEmpty())
        Files.writeString(root.resolve("a.txt"), body)
        finder.updateFile(Path.of("a.txt"))
        finder.removeFile(root.resolve("a.txt"))
        assertTrue(Files.exists(root.resolve("a.txt")))
    }

    @Test fun `directory scans can explicitly skip failed files with diagnostics`() {
        val finder = populated()
        Files.writeString(root.resolve("a.txt"), body)
        Files.writeString(root.resolve("bad.topic"), "<broken>")
        val summary = finder.indexDirectory(FileErrorPolicy.SKIP)
        assertEquals(1, summary.fileCount)
        assertEquals(1, summary.chunkCount)
        assertEquals(root.resolve("bad.topic"), summary.failures.single().path)
        assertTrue(summary.failures.single().message.isNotBlank())
        assertTrue(finder.chunksInFile(Path.of("b.txt")).isEmpty())
        assertTrue(finder.chunksInFile(Path.of("bad.topic")).isEmpty())
        assertFailsWith<UnsupportedOperationException> { (summary.failures as MutableList).clear() }
        val original = finder.report
        assertFailsWith<FileIndexingException> { finder.updateFile(Path.of("bad.topic")) }
        assertSame(original, finder.report)
    }

    @Test fun `source locations retain original XML offsets and consistent line bases`() {
        val finder = DuplicateFinder(options().copy(fileMask = mapOf(
            "topic" to ParserType.XML, "txt" to ParserType.LINE,
            "md" to ParserType.MARKDOWN, "properties" to ParserType.PROPERTIES,
            "adoc" to ParserType.ASCIIDOC,
        )))
        val xml = "\uFEFF<doc>\r\n<p>$body</p>\r</doc>"
        finder.updateContent(Path.of("a.topic"), xml)
        val location = finder.chunksInFile(Path.of("a.topic")).single().location
        assertEquals(2, location.line)
        val range = assertNotNull(location.sourceRange)
        assertEquals("<p>$body</p>", xml.substring(range.start, range.endExclusive))
        finder.updateContent(Path.of("a.txt"), "\n$body")
        finder.updateContent(Path.of("a.md"), "\n$body")
        finder.updateContent(Path.of("a.properties"), "# comment\nkey=$body")
        finder.updateContent(Path.of("a.adoc"), "\n$body")
        for (path in listOf("a.txt", "a.md", "a.properties", "a.adoc")) {
            assertEquals(2, finder.chunksInFile(Path.of(path)).single().location.line, path)
        }
    }

    @Test fun `heat maps handle empty and short text and UTF16 code units`() {
        val finder = DuplicateFinder(options().copy(minLength = 3))
        val text = "ab\uD83D\uDE00cd"
        finder.updateContent(Path.of("a.txt"), text)
        val result = finder.findFuzzy(FuzzyQuery.Text(text))
        val heatMap = finder.heatMap(result.referenceContent, result.matches)
        assertEquals(text.length, heatMap.scores.size)
        assertTrue(heatMap.scores.all { it == 1f })
        assertEquals(emptyList(), finder.heatMap("", result.matches).scores)
        assertEquals(listOf(0f, 0f), finder.heatMap("ab", result.matches).scores)
        assertTrue(finder.heatMap(text, emptyList()).scores.all { it == 0f })
        assertFailsWith<UnsupportedOperationException> { (heatMap.scores as MutableList).clear() }
    }

    @Test fun `seeded updates match fresh rebuilds with absent stale and fresh DF`() {
        val bodies = (0..5).map { randomBody(100L + it, 160) }
        for (threshold in listOf(0.85, 0.90, 0.95)) {
            val config = options(threshold)
            val live = DuplicateFinder(config)
            val contents = linkedMapOf<String, String>()
            val random = Random(424242)
            repeat(60) { step ->
                val path = "file${random.nextInt(8)}.txt"
                if (random.nextInt(4) == 0) {
                    contents.remove(path)
                    live.removeFile(Path.of(path))
                } else {
                    val base = bodies[random.nextInt(bodies.size)]
                    val text = if (random.nextBoolean()) base else "z" + base.drop(1)
                    contents[path] = text
                    live.updateContent(Path.of(path), text)
                }
                val fresh = DuplicateFinder(config)
                contents.forEach { (file, text) -> fresh.updateContent(Path.of(file), text) }
                val expected = canonical(fresh.report)
                assertEquals(expected, canonical(live.report), "threshold=$threshold step=$step")
                if (step % 7 == 0) {
                    val alternate = AnalysisOptions(threshold, minDuplicates = 2)
                    val expectedAlternate = canonical(fresh.analyze(alternate))
                    live.computeDf()
                    assertEquals(expected, canonical(live.report))
                    assertEquals(expectedAlternate, canonical(live.analyze(alternate)))
                }
                val query = FuzzyQuery.Text(bodies[step % bodies.size])
                assertEquals(matches(fresh.findFuzzy(query)), matches(live.findFuzzy(query)))
            }
        }
    }

    @Test fun `concurrent reports updates and maintenance publish coherent snapshots`() {
        val finder = populated(options(1.0))
        val pool = Executors.newFixedThreadPool(4)
        val barrier = CyclicBarrier(4)
        try {
            val work = (0 until 4).map { worker -> pool.submit(Callable {
                barrier.await(10, TimeUnit.SECONDS)
                var previousVersion = -1L
                repeat(50) { step ->
                    when (worker) {
                        0 -> finder.updateContent(Path.of("a.txt"), if (step % 2 == 0) body else body.reversed())
                        1 -> {
                            finder.updateContent(Path.of("b.txt"), body)
                            if (step % 3 == 0) finder.removeFile(Path.of("b.txt"))
                        }
                        2 -> finder.computeDf()
                        else -> {
                            val report = finder.report
                            assertTrue(report.indexVersion >= previousVersion)
                            previousVersion = report.indexVersion
                            report.duplicates.forEach { (ref, matches) ->
                                matches.forEach {
                                    assertEquals(ref.content, it.chunk.content)
                                    assertEquals(1.0, it.similarity)
                                    assertNotEquals(ref.id, it.chunk.id)
                                }
                            }
                            finder.findFuzzy(FuzzyQuery.Text(body)).matches.forEach { assertEquals(body, it.chunk.content) }
                        }
                    }
                }
            }) }
            work.forEach { it.get(30, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    @Test fun `directory replacements never expose a partially built index`() {
        repeat(4) { Files.writeString(root.resolve("$it.txt"), body) }
        val finder = DuplicateFinder(options())
        finder.indexDirectory()
        val pool = Executors.newFixedThreadPool(2)
        val barrier = CyclicBarrier(2)
        try {
            val rebuilds = pool.submit { barrier.await(); repeat(20) { finder.indexDirectory() } }
            val reads = pool.submit { barrier.await(); repeat(40) { assertEquals(4, finder.report.duplicates.size) } }
            rebuilds.get(30, TimeUnit.SECONDS)
            reads.get(30, TimeUnit.SECONDS)
        } finally {
            pool.shutdownNow()
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    private fun key(chunk: ChunkSnapshot): String =
        "${chunk.location.path}:${chunk.location.line}:${chunk.location.sourceRange}:${chunk.content}"

    private fun canonical(report: DuplicateFinderReport) = report.duplicates.entries.associate { (ref, duplicates) ->
        key(ref) to duplicates.map { key(it.chunk) to it.similarity }.sortedBy { it.first }
    }.toSortedMap()

    private fun matches(result: FuzzySearchResult) = result.matches.map { key(it.chunk) to it.similarity }.sortedBy { it.first }
}