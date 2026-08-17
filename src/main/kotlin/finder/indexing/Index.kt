package finder.indexing

import finder.*
import finder.ngram.NgramProvider
import finder.ngram.ngramProvider
import it.unimi.dsi.fastutil.ints.*
import it.unimi.dsi.fastutil.longs.LongArrays
import it.unimi.dsi.fastutil.objects.*
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.*
import kotlin.io.path.isRegularFile

class Index(
    val options: DuplicateFinderOptions,
    val ngramProvider: NgramProvider = ngramProvider(options),
) {

    private val lock = ReentrantReadWriteLock()
    private val registryMonitor = Any()

    private val directoryIndex = ConcurrentHashMap<Length, Int2ObjectOpenHashMap<IntArrayList>>()
    private val registry = Int2ObjectOpenHashMap<Chunk>()
    private val chunkIds = Object2IntOpenHashMap<Chunk>().apply { defaultReturnValue(-1) }
    private val pathToIds = Object2ObjectOpenHashMap<String, IntArrayList>()

    private val indexedContent = Int2ObjectOpenHashMap<String>()

    private val nextId = AtomicInteger(0)

    @Volatile
    private var df: Int2IntOpenHashMap? = null

    fun chunksFlat(): List<Chunk> = lock.read { ArrayList(registry.values) }

    fun duplicatesOf(reference: Chunk, options: DuplicateFinderOptions = this.options): List<Chunk> =
        lock.read { findForChunk(reference, this, options) }

    fun duplicatesInFile(path: String): Map<Chunk, List<Chunk>> = lock.read {
        val ids = pathToIds.get(path) ?: return@read emptyMap()
        val result = LinkedHashMap<Chunk, List<Chunk>>(ids.size)
        val idIt = ids.iterator()
        while (idIt.hasNext()) {
            val chunk = registry.get(idIt.nextInt()) ?: continue
            result[chunk] = findForChunk(chunk, this)
        }
        result
    }

    fun duplicatesInFile(path: Path): Map<Chunk, List<Chunk>> = duplicatesInFile(relativePath(path))

    fun allDuplicates(): Map<Chunk, List<Chunk>> = lock.read { findAll(this) }

    internal fun chunkForId(id: Int): Chunk? = registry.get(id)

    internal fun chunkId(chunk: Chunk): Int = chunkIds.getInt(chunk)

    internal fun bucketForLength(length: Int): Int2ObjectOpenHashMap<IntArrayList>? = directoryIndex[length]

    internal fun orderByFrequency(ngrams: IntSet): IntList {
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

    fun indexDirectory(): Unit = lock.write {
        val (root, _, _, _, _, verbose) = options
        val fileCount = AtomicInteger(0)
        val filesToIndex = filesToIndex(root, options)
        if (verbose) println("Indexing ${filesToIndex.size} files")

        filesToIndex.parallelStream()
            .peek { if (verbose) println("processing file ${fileCount.incrementAndGet()}: $it") }
            .forEach { path -> FileProcessor(options).fileToChunks(path).forEach { indexChunkExclusive(it) } }
    }

    fun computeDocFrequencies(): Unit = lock.write {
        val freq = Int2IntOpenHashMap()
        directoryIndex.values.forEach { ngramMap ->
            ngramMap.int2ObjectEntrySet().forEach { freq.addTo(it.intKey, it.value.size) }
        }
        df = freq
        if (options.verbose) println("Computed document frequencies for ${freq.size} distinct trigrams")
    }

    fun trim(): Unit = lock.write {
        directoryIndex.entries.removeIf { (_, ngramMap) ->
            ngramMap.int2ObjectEntrySet().removeIf { it.value.isEmpty }
            ngramMap.values.forEach { posting -> posting.trim() }
            ngramMap.trim()
            ngramMap.isEmpty()
        }
        registry.trim()
        chunkIds.trim()
        pathToIds.trim()
        indexedContent.trim()
        df?.trim()
    }

    fun reindexFile(path: String, chunks: List<Chunk>): Unit = lock.write {
        require(chunks.all { it.path == path }) {
            "chunks must be filed under the path being reindexed: expected '$path', " +
                    "got ${chunks.map { it.path }.distinct()}"
        }
        removeFileLocked(path)
        chunks.forEach { indexChunkExclusive(it) }
    }

    fun reindexFile(path: Path) {
        val chunks = FileProcessor(options).fileToChunks(path)
        reindexFile(relativePath(path), chunks)
    }

    fun reindexContent(content: String, path: Path) {
        val chunks = FileProcessor(options).contentToChunks(content, path)
        reindexFile(relativePath(path), chunks)
    }

    fun removeFile(path: String): Unit = lock.write { removeFileLocked(path) }

    fun removeFile(path: Path): Unit = removeFile(relativePath(path))

    fun removeChunksForPath(path: String): Unit = removeFile(path)

    fun clear(): Unit = lock.write {
        directoryIndex.clear()
        registry.clear()
        chunkIds.clear()
        pathToIds.clear()
        indexedContent.clear()
        df = null
        nextId.set(0)
    }

    fun indexChunk(chunk: Chunk): Unit = lock.write { indexChunkExclusive(chunk) }

    fun indexFile(path: Path) {
        val chunks = FileProcessor(options).fileToChunks(path)
        lock.write { chunks.forEach { indexChunkExclusive(it) } }
    }

    fun indexContent(content: String, path: Path) {
        val chunks = FileProcessor(options).contentToChunks(content, path)
        lock.write { chunks.forEach { indexChunkExclusive(it) } }
    }

    private fun indexChunkExclusive(chunk: Chunk) {
        val content = chunk.content
        val id = registerChunk(chunk, content).takeIf { it >= 0 } ?: return
        val ngrams = ngramProvider.ngrams(content)
        val forLength = getForLength(content.length)
        synchronized(forLength) {
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

    private fun registerChunk(chunk: Chunk, content: String): Int = synchronized(registryMonitor) {
        if (chunkIds.getInt(chunk) >= 0) return@synchronized -1
        val id = nextId.getAndIncrement()
        registry.put(id, chunk)
        chunkIds.put(chunk, id)
        indexedContent.put(id, content)
        var ids = pathToIds[chunk.path]
        if (ids == null) {
            ids = IntArrayList()
            pathToIds[chunk.path] = ids
        }
        ids.add(id)
        id
    }

    private fun removeFileLocked(path: String) = synchronized(registryMonitor) {
        val ids = pathToIds.remove(path) ?: return@synchronized
        val idIt = ids.iterator()
        while (idIt.hasNext()) {
            val id = idIt.nextInt()
            val chunk = registry.remove(id) ?: continue
            val content = indexedContent.remove(id) ?: chunk.content
            chunkIds.removeInt(chunk)
            val bucket = directoryIndex[content.length] ?: continue
            val ngramIt = ngramProvider.ngrams(content).iterator()
            synchronized(bucket) {
                while (ngramIt.hasNext()) bucket.get(ngramIt.nextInt())?.rem(id)
            }
        }
    }

    internal fun getForLength(length: Int): Int2ObjectOpenHashMap<IntArrayList> =
        directoryIndex.computeIfAbsent(length) { Int2ObjectOpenHashMap<IntArrayList>() }

    private fun relativePath(path: Path): String = options.root.relativize(path).toString()

    private fun filesToIndex(
        root: Path,
        options: DuplicateFinderOptions
    ) = Files.walk(root)
        .parallel()
        .filter { path -> path.isRegularFile() && options.fileMaskIncludes(path) }
        .toList()
}