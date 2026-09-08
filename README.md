# Duplicate Finder

A Kotlin/JVM library and desktop application for finding similar text passages.
Requires JDK 21.

| Module | Purpose |
| --- | --- |
| `core` | Public API, parsers, indexing, matching, immutable results, numerical heat maps |
| `app` | CLI, configuration, text-file export, Swing and Compose UIs |
| `example-client` | Small headless application depending only on core |

## Example client

Run the self-contained in-memory example:

```sh
./gradlew :example-client:run
```

Or scan `.topic` (XML) and `.txt` (line-based) files in a project:

```sh
./gradlew :example-client:run --args="/Users/igor.kulakov/IdeaProjects/jetbrains-desktop-products"
```

The [client source](example-client/src/main/kotlin/example/Main.kt) demonstrates
indexing, manual DF, reports, fuzzy search, heat maps, and removing a file from
the index. It never writes or deletes project files.

## Library API

```kotlin
val finder = DuplicateFinder(DuplicateFinderOptions(
    root = projectRoot,
    fileMask = mapOf("topic" to ParserType.XML),
    minLength = 251,
    analysis = AnalysisOptions(minSimilarity = 0.90),
))
finder.indexDirectory()
finder.computeDf() // Optional optimization; always caller-controlled.
val report = finder.report

finder.updateContent(path, unsavedText)
finder.updateFile(newFile)
finder.removeFile(deletedFile)
val updatedReport = finder.report

val result = finder.findFuzzy(FuzzyQuery.Text(selection), minSimilarity = 0.85)
val heatMap = finder.heatMap(result.referenceContent, result.matches)
```

Reports are immutable snapshots. The first report read after an update searches
the entire in-memory index; subsequent reads return the cached report. Fuzzy
search handles one query without computing the whole report. Neither operation
recomputes DF. `analyze(AnalysisOptions(...))` supports alternate report thresholds
without reindexing. All methods are synchronous and thread-safe; UI clients own
background scheduling.

The constructor performs no filesystem I/O. Directory indexing replaces the
index atomically; file updates add or replace contents. Failed reads or parses
leave the previous state intact. Relative paths resolve against the root.
For best-effort directory scans, use `indexDirectory(FileErrorPolicy.SKIP)` and
inspect the returned `failures`. The CLI and example use this policy and display
diagnostics for skipped files; individual updates always fail without replacing
previous contents when reading or parsing fails.
Returned chunk references remain usable across unrelated edits; searching a
reference whose file was replaced or removed throws `IllegalArgumentException`.
Heat maps can still be computed from old snapshots.

Core has no UI, CLI, or output dependencies. It can be consumed as a Gradle
project dependency or built as `core/build/libs/duplicate-finder-core-1.0.jar`.
Use the generated Maven publication for dependency metadata and sources:

```sh
./gradlew :core:build :core:generatePomFileForLibraryPublication
```

The publication coordinates are `dev.flounder:duplicate-finder-core:1.0`; this
repository does not imply that the artifact is available from a remote repository.

## Application

```sh
./gradlew :app:run --args="-r=/path/to/project -f=topic:xml -s=0.9 -l=251 -ui=none"
./gradlew fatJar
java -jar build/libs/duplicate-finder.jar -r=/path/to/project -ui=compose
```

Existing CLI flags and `duplicate-finder.properties` configuration are handled
by the application. `-ui=none` still exports text reports. Programmatic callers
can consume results directly without an output directory.

Run all tests with `./gradlew test`. The [API design document](docs/public-api-proposal.md)
provides the contract and migration rationale.