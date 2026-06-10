package finder.parsing

class LineParser : ContentParser() {

    override fun parse(content: String) = content.lines()
        .withIndex()
        .map { (number, line) -> Element(line, number, "line")}
        .toList()
}