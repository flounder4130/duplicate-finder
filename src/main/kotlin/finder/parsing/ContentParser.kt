package finder.parsing

abstract class ContentParser {
    abstract fun parse(content: String): List<Element>
}
