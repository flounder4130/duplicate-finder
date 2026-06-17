package finder.indexing

import finder.*
import finder.ngram.ngramProvider
import it.unimi.dsi.fastutil.ints.*
import it.unimi.dsi.fastutil.longs.LongArrays
import it.unimi.dsi.fastutil.objects.*
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntPredicate
import kotlin.io.path.isRegularFile

class Index(val options: DuplicateFinderOptions) {

    val ngramProvider = ngramProvider(options)

    private val directoryIndex = ConcurrentHashMap<Length, Int2ObjectOpenHashMap<IntArrayList>>()

    private val registry = ObjectArrayList<Chunk>()
    private val chunkIds = Object2IntOpenHashMap<Chunk>().apply { defaultReturnValue(-1) }

    @Volatile
    private var df: Int2IntOpenHashMap? = null

    fun chunkForId(id: Int): Chunk = registry.get(id)

    fun chunkId(chunk: Chunk): Int = chunkIds.getInt(chunk)

    private fun idForChunk(chunk: Chunk): Int = synchronized(registry) {
        var id = chunkIds.getInt(chunk)
        if (id < 0) {
            id = registry.size
            registry.add(chunk)
            chunkIds.put(chunk, id)
        }
        id
    }

    fun chunksFlat(): List<Chunk> {
        val ids = IntOpenHashSet()
        directoryIndex.values.forEach { ngramMap ->
            ngramMap.values.forEach { posting -> ids.addAll(posting) }
        }
        val result = ArrayList<Chunk>(ids.size)
        val it = ids.iterator()
        while (it.hasNext()) result.add(registry.get(it.nextInt()))
        return result
    }

    fun computeDocFrequencies() {
        val freq = Int2IntOpenHashMap()
        directoryIndex.values.forEach { ngramMap ->
            ngramMap.int2ObjectEntrySet().forEach { freq.addTo(it.intKey, it.value.size) }
        }
        df = freq
        if (options.verbose) println("Computed document frequencies for ${freq.size} distinct trigrams")
    }

    fun trim() {
        directoryIndex.values.forEach { ngramMap ->
            ngramMap.values.forEach { posting -> posting.trim() }
            ngramMap.trim()
        }
        df?.trim()
    }

    fun orderByFrequency(ngrams: IntSet): IntList {
        val ngramArray = ngrams.toIntArray()
        val freq = df ?: return IntArrayList.wrap(ngramArray)
        val packed = LongArray(ngramArray.size)
        for (i in ngramArray.indices) {
            val g = ngramArray[i]
            packed[i] = (freq.get(g).toLong() shl 32) or (g.toLong() and 0xFFFFFFFFL)
        }
        LongArrays.quickSort(packed)
        val result = IntArrayList(packed.size)
        for (i in packed.indices) {
            result.add((packed[i] and 0xFFFFFFFFL).toInt())
        }
        return result
    }

    fun getForLength(length: Int): Int2ObjectOpenHashMap<IntArrayList> =
        directoryIndex.computeIfAbsent(length) { Int2ObjectOpenHashMap<IntArrayList>() }

    fun bucketForLength(length: Int): Int2ObjectOpenHashMap<IntArrayList>? = directoryIndex[length]

    fun removeChunksForPath(path: String) {
        directoryIndex.values.forEach { ngramMap ->
            synchronized(ngramMap) {
                ngramMap.values.forEach { ids ->
                    ids.removeIf(IntPredicate { id -> registry.get(id).path == path })
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
        val id = idForChunk(chunk)
        val forLength = getForLength(chunk.content.length)
        synchronized (forLength) {
            val it = ngrams.iterator()
            while (it.hasNext()) {
                val ngram = it.nextInt()
                var forNgram = forLength.get(ngram)
                if (forNgram == null) {
                    forNgram = IntArrayList()
                    forLength.put(ngram, forNgram)
                }
                forNgram.add(id)
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