package finder.parsing

import finder.indexing.*

class JavaPropertiesParser : ContentParser() {
    override fun parse(content: String, path: String) = content.lines()
        .mapIndexed { index, line ->
            Pair(index + 1, line.trim())
        }
        .filter { (_, line) ->
            line.isNotEmpty() && !line.startsWith("#") && !line.startsWith("!")
        }
        .mapNotNull { (lineNumber, line) ->
            val separatorIndex = line.indexOf('=')
            if (separatorIndex > 0) {
                val value = line.substring(separatorIndex + 1).trim()
                PropertiesChunk(value, path, LineCoordinates(lineNumber))
            } else null
        }
}
