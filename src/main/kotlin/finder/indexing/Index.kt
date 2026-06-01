package finder.indexing

import finder.*
import finder.ngram.ngramProvider
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.isRegularFile

class Index private constructor(val options: DuplicateFinderOptions) {

    val ngramProvider = ngramProvider(options)

    companion object {
        @Volatile
        private var instance: Index? = null

        fun getInstance(options: DuplicateFinderOptions) = instance ?: synchronized(this) {
            instance ?: Index(options).also { instance = it }
        }

        fun resetInstance() {
            instance = null
        }
    }

    private val directoryIndex = ConcurrentHashMap<Length, MutableMap<Ngram, MutableList<Chunk>>>()

    @Volatile
    private var df: Object2IntOpenHashMap<Ngram>? = null

    fun chunksFlat(): List<Chunk> = directoryIndex.values.flatMap { it.values }.flatten().distinct()

    fun computeDocFrequencies() {
        val freq = Object2IntOpenHashMap<Ngram>()
        directoryIndex.values.forEach { ngramMap ->
            ngramMap.forEach { (ngram, chunks) -> freq.addTo(ngram, chunks.size) }
        }
        df = freq
        if (options.verbose) println("Computed document frequencies for ${freq.size} distinct trigrams")
    }

    fun orderByFrequency(ngrams: Set<Ngram>): List<Ngram> {
        val freq = df ?: return ngrams.toList()
        return ngrams.sortedBy { freq.getInt(it) }
    }

    fun getForLength(length: Int) = directoryIndex.computeIfAbsent(length) { mutableMapOf<Ngram, MutableList<Chunk>>() }

    fun removeChunksForPath(path: String) {
        directoryIndex.values.forEach { ngramMap ->
            synchronized(ngramMap) {
                ngramMap.values.forEach { chunks ->
                    chunks.removeIf { it.path == path }
                }
            }
        }
    }

    fun indexDirectory() {
        val (root, _, _, _, _, _, verbose) = options
        val fileCount = AtomicInteger(0)
        val filesToIndex = filesToIndex(root, options)
        if (verbose) println("Indexing ${filesToIndex.size} files")

        filesToIndex.parallelStream()
            .peek { if (verbose) println("processing file ${fileCount.incrementAndGet()}: $it") }
            .forEach { indexFile(it) }
    }

    fun indexFile(path: Path) {
        val fileProcessor = FileProcessor(options)
        val chunks = fileProcessor.fileToChunks(path)
        chunks.forEach { indexChunk(it) }
    }

    fun indexContent(content: String, path: Path) {
        val fileProcessor = FileProcessor(options)
        val chunks = fileProcessor.contentToChunks(content, path)
        chunks.forEach { indexChunk(it) }
    }

    fun indexChunk(chunk: Chunk) {
        val ngrams = ngramProvider.ngrams(chunk.content)
        val forLength = getForLength(chunk.content.length)
        synchronized (forLength) {
            ngrams.forEach { ngram ->
                val forNgram = forLength.computeIfAbsent(ngram) { mutableListOf() }
                forNgram.add(chunk)
            }
        }
    }

    private fun filesToIndex(
        root: Path,
        options: DuplicateFinderOptions
    ) = Files.walk(root)
        .parallel()
        .filter { path -> path.isRegularFile() && options.fileMaskIncludes(path) }
        .toList()
}