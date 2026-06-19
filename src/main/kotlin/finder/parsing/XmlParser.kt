package finder.parsing

import finder.indexing.*
import finder.parsing.xml.*

class XmlParser(
    private val inlineNested: Boolean,
    val skipTags: List<String> = emptyList(),
    private val captureOffsets: Boolean = false,
) : ContentParser() {

    override fun parse(content: String, path: String): List<Chunk> {
        val text = normalize(content)
        val root = Parser(text).parseDocument()
        val lines = LineIndex(text)
        val chunks = ArrayList<Chunk>()
        emit(root, path, lines, chunks)
        return chunks
    }

    private fun emit(node: RawNode, path: String, lines: LineIndex, chunks: MutableList<Chunk>) {
        for (child in node.children) {
            if (child.name != null) emit(child, path, lines, chunks)
        }
        val name = node.name ?: return
        if (name in skipTags) return
        val content = content(node)
        if (content.replace(NESTED_TAG_PLACEHOLDER, "").isNotBlank()) {
            chunks.add(XmlChunk(content.trim(), path, coordinates(node, lines), name))
        }
    }

    private fun coordinates(node: RawNode, lines: LineIndex): Coordinates {
        val line = lines.lineAt(node.start)
        return if (captureOffsets) {
            OffsetCoordinates(line, node.start, node.end, node.contentStart, node.contentEnd)
        } else {
            LineCoordinates(line)
        }
    }

    private fun content(node: RawNode): String =
        if (inlineNested) inlineContent(node, StringBuilder()).toString() else nonInlineContent(node)

    private fun inlineContent(node: RawNode, sb: StringBuilder): StringBuilder {
        val content = node.content
        if (content != null) return sb.append(content)
        for (child in node.children) {
            if (child.name == null) sb.append(child.content ?: "") else inlineContent(child, sb)
        }
        return sb
    }

    private fun nonInlineContent(node: RawNode): String {
        node.content?.let { return it }
        val sb = StringBuilder()
        for (child in node.children) {
            if (child.name == null) sb.append(child.content ?: "") else sb.append(NESTED_TAG_PLACEHOLDER)
        }
        return sb.toString()
    }

    private companion object {
        const val NESTED_TAG_PLACEHOLDER = "</>"

        fun normalize(content: String): String {
            val noBom = if (content.isNotEmpty() && content[0] == '﻿') content.substring(1) else content
            return if ('\r' in noBom) noBom.replace("\r\n", "\n").replace('\r', '\n') else noBom
        }
    }
}

private class LineIndex(text: String) {
    private val newlines: IntArray = run {
        var count = 0
        for (ch in text) if (ch == '\n') count++
        val offsets = IntArray(count)
        var next = 0
        for (i in text.indices) if (text[i] == '\n') offsets[next++] = i
        offsets
    }

    fun lineAt(offset: Int): Int {
        var lo = 0
        var hi = newlines.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (newlines[mid] < offset) lo = mid + 1 else hi = mid
        }
        return lo + 1
    }
}
