package finder

import finder.heatmap.charScores
import finder.indexing.*
import finder.model.*
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.*
import kotlin.concurrent.*
import kotlin.time.TimeSource

class DuplicateFinder(options: DuplicateFinderOptions) {
    val options: DuplicateFinderOptions = options.copy(
        root = options.root.toAbsolutePath().normalize(),
        fileMask = Collections.unmodifiableMap(LinkedHashMap(options.fileMask)),
    )

    private val mutations = ReentrantLock()
    private val state = ReentrantReadWriteLock()
    private val owner = UUID.randomUUID()
    private var index = Index(this.options)
    private var generation = 0L
    private var version = 0L
    private var frequenciesVersion: Long? = null
    private val snapshots = ConcurrentHashMap<Int, ChunkSnapshot>()

    @Volatile
    private var cachedReport: DuplicateFinderReport? = null

    val indexVersion: Long get() = state.read { version }
    val dfVersion: Long? get() = state.read { frequenciesVersion }

    val report: DuplicateFinderReport get() = analyze()

    fun indexDirectory(onError: FileErrorPolicy = FileErrorPolicy.FAIL): IndexingSummary = mutations.withLock {
        val started = TimeSource.Monotonic.markNow()
        val replacement = Index(options)
        val summary = replacement.indexDirectory(onError)
        replacement.trim()
        state.write {
            index = replacement
            generation++
            snapshots.clear()
            frequenciesVersion = null
            invalidate()
            IndexingSummary(version, summary.fileCount, index.chunkCount, started.elapsedNow(), summary.failures)
        }
    }

    fun updateFile(path: Path): UpdateSummary = update(path) { FileProcessor(options).fileToChunks(it) }

    fun updateContent(path: Path, content: String): UpdateSummary =
        update(path) { FileProcessor(options).contentToChunks(content, it) }

    fun removeFile(path: Path): UpdateSummary = mutations.withLock {
        val started = TimeSource.Monotonic.markNow()
        val relative = relativePath(resolvePath(path))
        state.write {
            val old = index.chunksInFile(relative)
            old.forEach { snapshots.remove(index.chunkId(it)) }
            index.removeFile(relative)
            if (old.isNotEmpty()) invalidate()
            UpdateSummary(version, old.isNotEmpty(), old.size, 0, started.elapsedNow())
        }
    }

    fun computeDf(): DfSummary = mutations.withLock {
        val started = TimeSource.Monotonic.markNow()
        state.write {
            index.trim()
            val distinct = index.computeDocFrequencies()
            frequenciesVersion = version
            DfSummary(version, distinct, started.elapsedNow())
        }
    }

    fun analyze(options: AnalysisOptions = this.options.analysis): DuplicateFinderReport = state.read {
        val cacheDefault = options == this.options.analysis
        if (cacheDefault) cachedReport?.let { return@read it }
        val started = TimeSource.Monotonic.markNow()
        val matches = index.allMatches(options)
        val entries = matches.keys.map { it to snapshot(it) }.sortedWith(compareBy(snapshotOrder) { it.second })
        val duplicates = entries.associateTo(LinkedHashMap()) { (chunk, reference) ->
            reference to publicMatches(matches.getValue(chunk))
        }
        DuplicateFinderReport(version, options, duplicates, started.elapsedNow()).also {
            if (cacheDefault) cachedReport = it
        }
    }

    fun chunksInFile(path: Path): List<ChunkSnapshot> = state.read {
        immutableList(index.chunksInFile(relativePath(resolvePath(path))).map(::snapshot).sortedWith(snapshotOrder))
    }

    fun findFuzzy(
        query: FuzzyQuery,
        minSimilarity: Double = options.analysis.minSimilarity,
    ): FuzzySearchResult {
        validateSimilarity(minSimilarity)
        return state.read {
            val reference = when (query) {
                is FuzzyQuery.Text -> LineChunk(
                    if (options.keepWhitespace) query.content else normalizeWhitespace(query.content),
                    "",
                    LineCoordinates(-1),
                )
                is FuzzyQuery.Reference -> {
                    val id = query.chunk.id
                    require(id.owner == owner && id.generation == generation) { "Reference belongs to another index snapshot" }
                    requireNotNull(index.chunkForId(id.value)) { "Reference has been removed or replaced" }
                }
            }
            FuzzySearchResult(version, reference.content, publicMatches(index.matchesOf(reference, minSimilarity)))
        }
    }

    fun heatMap(reference: ChunkSnapshot, duplicates: List<DuplicateMatch>): HeatMap =
        heatMap(reference.content, duplicates)

    fun heatMap(referenceContent: String, duplicates: List<DuplicateMatch>): HeatMap =
        HeatMap(referenceContent, charScores(options, referenceContent, duplicates.map { it.chunk.content }).toList())

    @Deprecated("Call indexDirectory(), optionally computeDf(), then read report")
    fun run(): DuplicateFinderReport {
        indexDirectory()
        computeDf()
        return report
    }

    private fun update(path: Path, parse: (Path) -> List<Chunk>): UpdateSummary = mutations.withLock {
        val started = TimeSource.Monotonic.markNow()
        val absolute = resolvePath(path)
        require(options.fileMaskIncludes(absolute)) { "No parser configured for $path" }
        val chunks = parse(absolute)
        val relative = relativePath(absolute)
        state.write {
            val old = index.chunksInFile(relative)
            old.forEach { snapshots.remove(index.chunkId(it)) }
            index.reindexFile(relative, chunks)
            val added = index.chunksInFile(relative).size
            val changed = old.isNotEmpty() || added > 0
            if (changed) invalidate()
            UpdateSummary(version, changed, old.size, added, started.elapsedNow())
        }
    }

    private fun invalidate() {
        version++
        cachedReport = null
    }

    private fun resolvePath(path: Path): Path {
        val absolute = options.root.resolve(path).normalize()
        require(absolute.startsWith(options.root) && absolute != options.root) { "Path must be a file inside ${options.root}: $path" }
        return absolute
    }

    private fun relativePath(path: Path): String = options.root.relativize(path).toString()

    private fun snapshot(chunk: Chunk): ChunkSnapshot {
        val id = index.chunkId(chunk)
        return snapshots.computeIfAbsent(id) {
            val line = when (val coordinates = chunk.coordinates) {
                is OffsetCoordinates -> coordinates.line
                is LineCoordinates -> coordinates.lineNumber +
                    if (chunk is LineChunk || chunk is MdChunk || chunk is FileChunk) 1 else 0
            }
            ChunkSnapshot(
                ChunkId(owner, generation, id),
                chunk.content,
                SourceLocation(Path.of(chunk.path), line, chunk.sourceRange),
            )
        }
    }

    private fun publicMatches(matches: List<ScoredChunk>): List<DuplicateMatch> =
        matches.map { DuplicateMatch(snapshot(it.chunk), it.similarity) }
            .sortedWith(compareByDescending<DuplicateMatch> { it.similarity }.thenComparing { a, b ->
                snapshotOrder.compare(a.chunk, b.chunk)
            })

    private companion object {
        val snapshotOrder: Comparator<ChunkSnapshot> =
            compareBy<ChunkSnapshot> { it.location.path.toString().replace('\\', '/') }
                .thenBy { it.location.line }
                .thenBy { it.location.sourceRange?.start ?: -1 }
                .thenBy { it.location.sourceRange?.endExclusive ?: -1 }
                .thenBy { it.content }
    }
}