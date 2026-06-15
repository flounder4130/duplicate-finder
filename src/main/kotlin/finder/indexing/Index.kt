package finder.indexing

import finder.*
import finder.ngram.ngramProvider
import it.unimi.dsi.fastutil.ints.*
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.isRegularFile

class Index(val options: DuplicateFinderOptions) {

    val ngramProvider = ngramProvider(options)

    private val directoryIndex = ConcurrentHashMap<Length, Int2ObjectOpenHashMap<MutableList<Chunk>>>()

    @Volatile
    private var df: Int2IntOpenHashMap? = null

    fun chunksFlat(): List<Chunk> = directoryIndex.values.flatMap { it.values }.flatten().distinct()

    fun computeDocFrequencies() {
        val freq = Int2IntOpenHashMap()
        directoryIndex.values.forEach { ngramMap ->
            ngramMap.int2ObjectEntrySet().forEach { freq.addTo(it.intKey, it.value.size) }
        }
        df = freq
        if (options.verbose) println("Computed document frequencies for ${freq.size} distinct trigrams")
    }

    fun orderByFrequency(ngrams: IntSet): IntList {
        val arr = ngrams.toIntArray()
        val freq = df ?: return IntArrayList.wrap(arr)
        IntArrays.quickSort(arr) { a, b -> freq.get(a) - freq.get(b) }
        return IntArrayList.wrap(arr)
    }

    fun getForLength(length: Int): Int2ObjectOpenHashMap<MutableList<Chunk>> =
        directoryIndex.computeIfAbsent(length) { Int2ObjectOpenHashMap<MutableList<Chunk>>() }

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
        val (root, _, _, _, _, verbose) = options
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
            val it = ngrams.iterator()
            while (it.hasNext()) {
                val ngram = it.nextInt()
                var forNgram = forLength.get(ngram)
                if (forNgram == null) {
                    forNgram = mutableListOf()
                    forLength.put(ngram, forNgram)
                }
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