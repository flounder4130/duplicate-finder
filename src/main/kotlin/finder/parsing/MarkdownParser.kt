package finder.parsing

import org.commonmark.node.*
import org.commonmark.parser.*
import org.commonmark.renderer.text.TextContentRenderer

class MarkdownParser : ContentParser() {

    override fun parse(content: String): List<Element> {
        val document = Parser
            .builder()
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build()
            .parse(content)

        val elements = mutableListOf<Element>()

        fun addElement(markdownBlock: Block, type: String) {
            val content = TextContentRenderer.builder().build().render(markdownBlock)
            elements.add(Element(
                content = content,
                lineNumber = markdownBlock.sourceSpans.first().lineIndex,
                type = type
            ))
        }

        document.accept(object : AbstractVisitor() {
            override fun visit(paragraph: Paragraph) = addElement(paragraph, "md_paragraph")
            override fun visit(fencedCodeBlock: FencedCodeBlock) = addElement(fencedCodeBlock, "md_fenced_code")
            override fun visit(heading: Heading) = addElement(heading, "md_heading")
            override fun visit(orderedList: OrderedList) = addElement(orderedList, "md_ordered_list")
            override fun visit(bulletList: BulletList) = addElement(bulletList, "md_bullet_list")
            override fun visit(listItem: ListItem) = addElement(listItem, "md_list_item")
            override fun visit(blockQuote: BlockQuote) = addElement(blockQuote, "md_block_quote")
            override fun visit(indentedCodeBlock: IndentedCodeBlock) = addElement(indentedCodeBlock, "md_indented_code")
            override fun visit(thematicBreak: ThematicBreak) = addElement(thematicBreak, "md_thematic_break")
            override fun visit(htmlBlock: HtmlBlock) = addElement(htmlBlock, "md_html_block")
            override fun visit(customBlock: CustomBlock) = addElement(customBlock, "md_custom_block")
        })

        return elements
    }
}