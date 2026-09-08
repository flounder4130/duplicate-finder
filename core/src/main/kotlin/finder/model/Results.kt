package finder.model

import finder.AnalysisOptions
import java.nio.file.Path
import java.util.Collections
import java.util.UUID
import kotlin.time.Duration

class ChunkId internal constructor(
    internal val owner: UUID,
    internal val generation: Long,
    internal val value: Int,
) {
    override fun equals(other: Any?): Boolean = other is ChunkId &&
        owner == other.owner && generation == other.generation && value == other.value

    override fun hashCode(): Int = 31 * (31 * owner.hashCode() + generation.hashCode()) + value
}

data class TextRange(val start: Int, val endExclusive: Int) {
    init { require(start >= 0 && endExclusive >= start) }
}

class SourceLocation internal constructor(
    val path: Path,
    val line: Int,
    val sourceRange: TextRange?,
)

class ChunkSnapshot internal constructor(
    val id: ChunkId,
    val content: String,
    val location: SourceLocation,
) {
    override fun equals(other: Any?): Boolean = other is ChunkSnapshot && id == other.id
    override fun hashCode(): Int = id.hashCode()
    override fun toString(): String = "${location.path}:${location.line}"
}

class DuplicateMatch internal constructor(val chunk: ChunkSnapshot, val similarity: Double)

class DuplicateFinderReport internal constructor(
    val indexVersion: Long,
    val options: AnalysisOptions,
    duplicates: Map<ChunkSnapshot, List<DuplicateMatch>>,
    val analysisDuration: Duration,
) {
    val duplicates: Map<ChunkSnapshot, List<DuplicateMatch>> = Collections.unmodifiableMap(
        duplicates.mapValuesTo(LinkedHashMap()) { (_, matches) -> immutableList(matches) }
    )
}

class FuzzySearchResult internal constructor(
    val indexVersion: Long,
    val referenceContent: String,
    matches: List<DuplicateMatch>,
) {
    val matches: List<DuplicateMatch> = immutableList(matches)
}

class HeatMap internal constructor(val content: String, scores: List<Float>) {
    val scores: List<Float> = immutableList(scores)
}

data class FileDiagnostic(val path: Path, val message: String)

class IndexingSummary internal constructor(
    val indexVersion: Long,
    val fileCount: Int,
    val chunkCount: Int,
    val duration: Duration,
    failures: List<FileDiagnostic>,
) {
    val failures: List<FileDiagnostic> = immutableList(failures)
}
data class UpdateSummary(
    val indexVersion: Long,
    val changed: Boolean,
    val removedChunks: Int,
    val addedChunks: Int,
    val duration: Duration,
)
data class DfSummary(val indexVersion: Long, val distinctNgrams: Int, val duration: Duration)

internal fun <T> immutableList(values: Collection<T>): List<T> = Collections.unmodifiableList(ArrayList(values))