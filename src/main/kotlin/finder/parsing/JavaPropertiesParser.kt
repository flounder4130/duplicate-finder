package finder.parsing

import finder.DuplicateFinderOptions

class JavaPropertiesParser(options: DuplicateFinderOptions) : ContentParser(options) {
    override fun parse(content: String): List<Element> = content.lines()
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
                if (value.length >= minLength) {
                    Element(value, lineNumber, "java_property")
                } else null
            } else null
        }
}
