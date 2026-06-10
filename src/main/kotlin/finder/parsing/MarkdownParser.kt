package finder.parsing

import finder.indexing.*
import org.commonmark.node.*
import org.commonmark.parser.*
import org.commonmark.renderer.text.TextContentRenderer

class MarkdownParser : ContentParser() {

    override fun parse(content: String, path: String): List<Chunk> {
        val document = Parser
            .builder()
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build()
            .parse(content)

        val chunks = mutableListOf<Chunk>()

        fun addBlock(markdownBlock: Block, blockType: String) {
            val blockContent = TextContentRenderer.builder().build().render(markdownBlock)
            val coordinates = LineCoordinates(markdownBlock.sourceSpans.first().lineIndex)
            chunks.add(MdChunk(blockContent, path, coordinates, blockType))
        }

        document.accept(object : AbstractVisitor() {
            override fun visit(paragraph: Paragraph) = addBlock(paragraph, "paragraph")
            override fun visit(fencedCodeBlock: FencedCodeBlock) = addBlock(fencedCodeBlock, "fenced_code")
            override fun visit(heading: Heading) = addBlock(heading, "heading")
            override fun visit(orderedList: OrderedList) = addBlock(orderedList, "ordered_list")
            override fun visit(bulletList: BulletList) = addBlock(bulletList, "bullet_list")
            override fun visit(listItem: ListItem) = addBlock(listItem, "list_item")
            override fun visit(blockQuote: BlockQuote) = addBlock(blockQuote, "block_quote")
            override fun visit(indentedCodeBlock: IndentedCodeBlock) = addBlock(indentedCodeBlock, "indented_code")
            override fun visit(thematicBreak: ThematicBreak) = addBlock(thematicBreak, "thematic_break")
            override fun visit(htmlBlock: HtmlBlock) = addBlock(htmlBlock, "html_block")
            override fun visit(customBlock: CustomBlock) = addBlock(customBlock, "custom_block")
        })

        return chunks
    }
}