package finder.indexing

import finder.*
import finder.ngram.ngramProvider
import finder.parsing.ParserType
import finder.similarity.similarityRatio
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexConcurrencyTest {

    private val options = mockOptionsForNgramLength(3).copy(minSimilarity = 0.9)

    private fun shared(slot: Int) = randomBody(100L + slot, SHARED_LENGTH)

    private fun unique(file: Int, slot: Int) =
        randomBody(10_000L + file * 100L + slot, 236 + ((file * SLOTS + slot) % 9) * 4)

    private fun nearMiss(slot: Int) = nearMissOf(shared(slot))

    private fun path(file: Int) = "file$file.topic"

    private fun chunk(content: String, path: String, slot: Int) = LineChunk(content, path, LineCoordinates(slot))

    private fun chunksFor(file: Int, state: Int): List<Chunk> = when (state) {
        ABSENT -> emptyList()
        SHARED -> (0 until SLOTS).map { chunk(shared(it), path(file), it) }
        else -> (0 until SLOTS).map { chunk(unique(file, it), path(file), it) }
    }

    private fun sharedProbe(slot: Int) = chunk(shared(slot), "probe", -1 - slot)

    private fun nearMissProbe(slot: Int) = chunk(nearMiss(slot), "probe", -1 - slot)

    private fun key(chunk: Chunk) = "${chunk.path}#${chunk.coordinates}=${chunk.content}"

    private fun canonical(duplicates: Map<Chunk, List<Chunk>>) =
        duplicates.entries.associate { (ref, dups) -> key(ref) to dups.map(::key).sorted() }.toSortedMap()

    @Test
    fun `fixtures discriminate`() {
        val ngrams = ngramProvider(options)
        val bodies = buildList {
            for (slot in 0 until SLOTS) add("shared$slot" to shared(slot))
            for (file in 0 until FILES) for (slot in 0 until SLOTS) add("unique$file-$slot" to unique(file, slot))
        }

        for (i in bodies.indices) for (j in i + 1 until bodies.size) {
            val (leftName, left) = bodies[i]
            val (rightName, right) = bodies[j]
            val similarity = similarityRatio(ngrams.ngrams(left), ngrams.ngrams(right))
            assertTrue(
                similarity < options.minSimilarity,
                "$leftName and $rightName are ${"%.2f".format(similarity)} similar — the stress test could not " +
                        "tell a stale hit from a legitimate one"
            )
        }

        for (slot in 0 until SLOTS) {
            assertEquals(
                shared(slot).length, nearMiss(slot).length,
                "the near-miss for slot $slot must stay in the same length bucket as its base"
            )
            val similarity = similarityRatio(ngrams.ngrams(shared(slot)), ngrams.ngrams(nearMiss(slot)))
            assertTrue(
                similarity >= options.minSimilarity / 2 && similarity < options.minSimilarity,
                "the near-miss for slot $slot is ${"%.2f".format(similarity)} similar, outside " +
                        "[${options.minSimilarity / 2}, ${options.minSimilarity})"
            )
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun `concurrent reindexing and searching stays consistent`() {
        val index = Index(options)
        val states = AtomicIntegerArray(FILES)
        for (file in 0 until FILES) {
            states.set(file, file % 2)
            index.reindexFile(path(file), chunksFor(file, states.get(file)))
        }
        index.computeDocFrequencies()

        val knownBodies = buildSet {
            for (slot in 0 until SLOTS) add(shared(slot))
            for (file in 0 until FILES) for (slot in 0 until SLOTS) add(unique(file, slot))
        }

        val errors = ConcurrentLinkedQueue<Throwable>()
        val stop = AtomicBoolean(false)
        val writeCount = AtomicLong()
        val readCount = AtomicLong()
        val barrier = CyclicBarrier(WRITERS + READERS + 2) // + maintenance + main

        val writers = List(WRITERS) { w ->
            thread(name = "reindex-$w") {
                guarded(errors, barrier) {
                    val rnd = Random(w.toLong())
                    val owned = (w until FILES step WRITERS).toList()
                    repeat(FLIPS_PER_WRITER) {
                        val file = owned[rnd.nextInt(owned.size)]
                        val next = rnd.nextInt(3)
                        states.set(file, next)
                        if (next == ABSENT) index.removeFile(path(file))
                        else index.reindexFile(path(file), chunksFor(file, next))
                        writeCount.incrementAndGet()
                    }
                }
            }
        }

        val readers = List(READERS) { r ->
            thread(name = "search-$r") {
                guarded(errors, barrier) {
                    val rnd = Random(1_000L + r)
                    while (!stop.get()) {
                        when (rnd.nextInt(4)) {
                            0 -> {
                                val slot = rnd.nextInt(SLOTS)
                                index.duplicatesOf(sharedProbe(slot)).forEach {
                                    assertEquals(
                                        shared(slot), it.content,
                                        "a search for slot $slot returned ${it.path}, whose body is not that slot's"
                                    )
                                }
                            }

                            1 -> {
                                val slot = rnd.nextInt(SLOTS)
                                val hits = index.duplicatesOf(nearMissProbe(slot))
                                assertTrue(
                                    hits.isEmpty(),
                                    "the near-miss probe for slot $slot matched ${hits.map { it.path }} — some " +
                                            "chunk is being counted more than once in a posting list"
                                )
                            }

                            2 -> {
                                val flat = index.chunksFlat()
                                assertTrue(
                                    flat.size <= FILES * SLOTS,
                                    "the index holds ${flat.size} chunks, more than the ${FILES * SLOTS} that exist"
                                )
                                flat.forEach {
                                    assertTrue(it.content in knownBodies, "unknown body indexed under ${it.path}")
                                }
                            }

                            else -> {
                                val file = rnd.nextInt(FILES)
                                index.duplicatesInFile(path(file)).forEach { (ref, dups) ->
                                    dups.forEach {
                                        assertEquals(ref.content, it.content, "$it is not a duplicate of $ref")
                                    }
                                }
                            }
                        }
                        readCount.incrementAndGet()
                    }
                }
            }
        }

        val maintenance = thread(name = "maintenance") {
            guarded(errors, barrier) {
                while (!stop.get()) {
                    index.computeDocFrequencies()
                    index.trim()
                    Thread.sleep(2)
                }
            }
        }

        barrier.await(30, TimeUnit.SECONDS)
        writers.forEach { it.join(JOIN_TIMEOUT_MS) }
        stop.set(true)
        (readers + maintenance).forEach { it.join(JOIN_TIMEOUT_MS) }
        (writers + readers + maintenance).forEach {
            assertFalse(it.isAlive, "${it.name} is stuck after ${JOIN_TIMEOUT_MS}ms — deadlock or livelock")
        }
        assertTrue(errors.isEmpty(), errors.joinToString("\n\n") { it.stackTraceToString() })
        assertTrue(writeCount.get() > 0 && readCount.get() > 0, "the test did no work")

        val reference = Index(options)
        for (file in 0 until FILES) reference.reindexFile(path(file), chunksFor(file, states.get(file)))
        reference.computeDocFrequencies()

        assertEquals(
            reference.chunksFlat().map(::key).sorted(),
            index.chunksFlat().map(::key).sorted(),
            "the live index holds different chunks than a from-scratch index of the same content"
        )
        for (slot in 0 until SLOTS) {
            assertEquals(
                reference.duplicatesOf(sharedProbe(slot)).map(::key).sorted(),
                index.duplicatesOf(sharedProbe(slot)).map(::key).sorted(),
                "a search for slot $slot disagrees with a from-scratch index"
            )
        }
        for (file in 0 until FILES) {
            assertEquals(
                canonical(reference.duplicatesInFile(path(file))),
                canonical(index.duplicatesInFile(path(file))),
                "the duplicates in ${path(file)} disagree with a from-scratch index"
            )
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun `full reindex and searching do not deadlock`(@TempDir root: Path) {
        val bodies = (0 until FILES).map { file ->
            (0 until SLOTS).joinToString("\n") { slot -> if (file % 3 == 0) shared(slot) else unique(file, slot) }
        }
        bodies.forEachIndexed { file, text -> Files.writeString(root.resolve("file$file.txt"), text) }

        val fileOptions = options.copy(root = root, fileMask = mapOf("txt" to ParserType.LINE))
        val index = Index(fileOptions)
        index.indexDirectory()
        index.computeDocFrequencies()

        val knownBodies = bodies.flatMap { it.lines() }.toSet()
        val errors = ConcurrentLinkedQueue<Throwable>()
        val stop = AtomicBoolean(false)
        val barrier = CyclicBarrier(READERS + 2)

        val rebuilder = thread(name = "rebuild") {
            guarded(errors, barrier) {
                repeat(REBUILDS) {
                    index.clear()
                    index.indexDirectory()
                    index.computeDocFrequencies()
                }
            }
        }
        val readers = List(READERS) { r ->
            thread(name = "search-$r") {
                guarded(errors, barrier) {
                    val rnd = Random(7_000L + r)
                    while (!stop.get()) {
                        val slot = rnd.nextInt(SLOTS)
                        index.duplicatesOf(sharedProbe(slot)).forEach {
                            assertEquals(shared(slot), it.content, "a rebuild handed out a body from another slot")
                        }
                        index.chunksFlat().forEach {
                            assertTrue(it.content in knownBodies, "unknown body indexed under ${it.path}")
                        }
                        index.allDuplicates()
                    }
                }
            }
        }

        barrier.await(30, TimeUnit.SECONDS)
        rebuilder.join(JOIN_TIMEOUT_MS)
        stop.set(true)
        readers.forEach { it.join(JOIN_TIMEOUT_MS) }
        (readers + rebuilder).forEach {
            assertFalse(it.isAlive, "${it.name} is stuck after ${JOIN_TIMEOUT_MS}ms — deadlock or livelock")
        }
        assertTrue(errors.isEmpty(), errors.joinToString("\n\n") { it.stackTraceToString() })

        val reference = Index(fileOptions)
        reference.indexDirectory()
        assertEquals(reference.chunksFlat().map(::key).sorted(), index.chunksFlat().map(::key).sorted())
    }

    private fun guarded(errors: MutableCollection<Throwable>, barrier: CyclicBarrier, body: () -> Unit) {
        try {
            barrier.await(30, TimeUnit.SECONDS)
            body()
        } catch (t: Throwable) {
            errors.add(t)
        }
    }

    private companion object {
        const val FILES = 12
        const val SLOTS = 5
        const val WRITERS = 4
        const val READERS = 4
        const val FLIPS_PER_WRITER = 2_500
        const val REBUILDS = 30
        const val JOIN_TIMEOUT_MS = 60_000L
        const val SHARED_LENGTH = 240

        const val SHARED = 0
        const val ABSENT = 2
    }
}