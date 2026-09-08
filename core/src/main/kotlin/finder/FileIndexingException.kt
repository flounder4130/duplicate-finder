package finder

import java.nio.file.Path

class FileIndexingException(val path: Path, cause: Exception) :
    RuntimeException("Cannot index $path: ${cause.message}", cause)