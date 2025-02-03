package finder.parsing

import finder.DuplicateFinderOptions

class AsciiDocParser(options: DuplicateFinderOptions): ContentParser(options) {

    override fun parse(content: String): List<Element> {
        val lines = content.lines()
        val elements = mutableListOf<Element>()
        var currentBlock = mutableListOf<Pair<String, Int>>()
        var inCodeBlock = false
        var inTable = false
        var tableHeader = true
        var skipUntilLine = -1  // Track which lines to skip for multi-line table rows

        fun addBlock(content: String, startLine: Int, type: String) {
            if (content.length >= minLength) {
                elements.add(Element(content, startLine, type))
            }
        }

        fun processCurrentBlock() {
            if (currentBlock.isEmpty()) return

            val content = currentBlock.joinToString("\n") { it.first }.trim()
            val startLine = currentBlock.first().second
            if (content.isNotEmpty()) {
                when {
                    content.startsWith("= ") -> {
                        // Extract only the first line as the document title
                        val title = content.removePrefix("= ").lines().first().trim()
                        addBlock(title, startLine, "adoc_section_0")

                        // Process remaining lines as a separate paragraph if they exist
                        val remainingLines = content.lines().drop(1).joinToString("\n").trim()
                        if (remainingLines.isNotEmpty()) {
                            addBlock(remainingLines, startLine + 1, "adoc_metadata")
                        }
                    }
                    content.startsWith("== ") -> addBlock(content.removePrefix("== ").trim(), startLine, "adoc_section_1")
                    content.startsWith("=== ") -> addBlock(content.removePrefix("=== ").trim(), startLine, "adoc_section_2")
                    content.startsWith("==== ") -> addBlock(content.removePrefix("==== ").trim(), startLine, "adoc_section_3")
                    content.startsWith("* ") || content.startsWith("** ") -> {
                        content.lines().forEachIndexed { index, line ->
                            val itemContent = line.trim().removePrefix("*").removePrefix("*").trim()
                            if (itemContent.isNotEmpty()) {
                                addBlock(itemContent, startLine + index, "adoc_list_item")
                            }
                        }
                    }
                    else -> {
                        // Split content into lines and check for embedded lists
                        val lines = content.lines()
                        var currentParagraph = mutableListOf<String>()
                        var currentLineNumber = startLine

                        lines.forEach { line ->
                            if (line.trim().startsWith("* ")) {
                                // If we have accumulated paragraph content, add it first
                                if (currentParagraph.isNotEmpty()) {
                                    addBlock(currentParagraph.joinToString("\n"), currentLineNumber, "adoc_paragraph")
                                    currentParagraph.clear()
                                }
                                // Add the list item
                                val itemContent = line.trim().removePrefix("*").trim()
                                if (itemContent.isNotEmpty()) {
                                    addBlock(itemContent, currentLineNumber, "adoc_list_item")
                                }
                            } else {
                                currentParagraph.add(line)
                            }
                            currentLineNumber++
                        }

                        // Add any remaining paragraph content
                        if (currentParagraph.isNotEmpty()) {
                            addBlock(currentParagraph.joinToString("\n"), startLine, "adoc_paragraph")
                        }
                    }
                }
            }
            currentBlock.clear()
        }

        lines.forEachIndexed { lineNumber, line ->
            when {
                line.startsWith(".") -> {
                    processCurrentBlock()
                    addBlock(line.removePrefix(".").trim(), lineNumber + 1, "adoc_table_title")
                }
                line == "|===" -> {
                    processCurrentBlock()
                    inTable = !inTable
                    tableHeader = true
                }
                line == "----" -> {
                    if (inCodeBlock) {
                        val codeContent = currentBlock.joinToString("\n") { it.first }
                        if (codeContent.isNotEmpty()) {
                            addBlock(codeContent, currentBlock.first().second, "adoc_listing")
                        }
                        currentBlock.clear()
                    }
                    inCodeBlock = !inCodeBlock
                }
                inTable && line.trim().isNotEmpty() -> {
                    if (tableHeader) {
                        // Clean up header content by removing pipe characters and trimming
                        val headerContent = line.trim()
                            .split("|")
                            .filter { it.isNotEmpty() }
                            .joinToString(" | ") { it.trim() }
                        addBlock(headerContent, lineNumber + 1, "adoc_table_header")
                        tableHeader = false
                    } else if (line.startsWith("|")) {
                        // Skip table boundary markers and already processed lines
                        if (!line.contains("===") && lineNumber >= skipUntilLine) {
                            // Collect all cells until empty line or non-cell line
                            val cells = mutableListOf<String>()
                            var currentLineIndex = lineNumber
                            var currentLine = line

                            // Process current line and look ahead for additional cells
                            while (currentLine.startsWith("|") && !currentLine.contains("===")) {
                                cells.addAll(currentLine.split("|")
                                    .filter { it.isNotEmpty() }
                                    .map { it.trim() })

                                val nextLine = lines.getOrNull(currentLineIndex + 1)
                                if (nextLine == null || nextLine.trim().isEmpty() || !nextLine.startsWith("|")) {
                                    break
                                }
                                currentLineIndex++
                                currentLine = nextLine
                            }

                            if (cells.isNotEmpty()) {
                                addBlock(cells.joinToString(" | "), lineNumber + 1, "adoc_table_row")
                                // Set the skip line to after the last processed line
                                skipUntilLine = currentLineIndex + 1
                            }
                        }
                    }
                }
                inCodeBlock -> currentBlock.add(line to (lineNumber + 1))
                line.trim().isEmpty() -> processCurrentBlock()
                line.startsWith("[source") -> {
                    processCurrentBlock()
                }
                else -> currentBlock.add(line to (lineNumber + 1))
            }
        }

        processCurrentBlock()
        return elements
    }
}
