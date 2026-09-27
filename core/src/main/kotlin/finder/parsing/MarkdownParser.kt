package finder.parsing

import finder.indexing.*
import finder.model.TextRange
import org.commonmark.node.*
import org.commonmark.parser.*
import org.commonmark.renderer.text.TextContentRenderer

internal class MarkdownParser : ContentParser() {

    override fun parse(content: String, path: String): List<Chunk> {
        val document = Parser
            .builder()
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build()
            .parse(content)

        val chunks = mutableListOf<Chunk>()
        val renderer = TextContentRenderer.builder().build()
        val lineOffsets = sourceLineOffsets(content)

        fun addBlock(markdownBlock: Block, blockType: String) {
            val firstSpan = markdownBlock.sourceSpans.firstOrNull() ?: return
            val lastSpan = markdownBlock.sourceSpans.last()
            val blockContent = renderer.render(markdownBlock)
            if (blockContent.isBlank()) return
            val coordinates = LineCoordinates(firstSpan.lineIndex)
            chunks.add(MdChunk(blockContent, path, coordinates, blockType).apply {
                sourceRange = TextRange(
                    lineOffsets[firstSpan.lineIndex] + firstSpan.columnIndex,
                    lineOffsets[lastSpan.lineIndex] + lastSpan.columnIndex + lastSpan.length,
                )
            })
        }

        // Lists and quotes are containers: visit their children without indexing the same text twice.
        document.accept(object : AbstractVisitor() {
            override fun visit(paragraph: Paragraph) = addBlock(paragraph, "paragraph")
            override fun visit(fencedCodeBlock: FencedCodeBlock) = addBlock(fencedCodeBlock, "fenced_code")
            override fun visit(heading: Heading) = addBlock(heading, "heading")
            override fun visit(indentedCodeBlock: IndentedCodeBlock) = addBlock(indentedCodeBlock, "indented_code")
            override fun visit(htmlBlock: HtmlBlock) = addBlock(htmlBlock, "html_block")
        })

        return chunks
    }

    // CommonMark spans use line/column indices; retain the original UTF-16 offsets and line endings.
    private fun sourceLineOffsets(content: String): List<Int> {
        val offsets = mutableListOf(0)
        var offset = 0
        while (offset < content.length) {
            when (content[offset++]) {
                '\r' -> {
                    if (content.getOrNull(offset) == '\n') offset++
                    offsets.add(offset)
                }
                '\n' -> offsets.add(offset)
            }
        }
        return offsets
    }
}