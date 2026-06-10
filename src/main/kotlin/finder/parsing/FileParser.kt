package finder.parsing

class FileParser : ContentParser() {

    override fun parse(content: String) = listOf(Element(content, 0, "entire_file"))
}