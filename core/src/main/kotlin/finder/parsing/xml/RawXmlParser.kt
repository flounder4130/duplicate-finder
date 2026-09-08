package finder.parsing.xml

internal const val NO_OFFSET = -1

internal class RawNode(
    val name: String?,
    val attributes: Map<String, String>,
    val attrOffsets: Map<String, IntRange>,
    val children: List<RawNode>,
    var content: String?,
    val start: Int,
    val end: Int,
    val contentStart: Int,
    val contentEnd: Int,
)

internal class XmlParseException(message: String) : RuntimeException(message)

private const val NO = NO_OFFSET

internal class Parser(private val s: String) {
    private val len = s.length
    private var pos = 0

    fun parseDocument(): RawNode {
        while (pos < len) {
            skipSpaces()
            if (pos >= len) break
            val c = s[pos]
            if (c != '<') throw XmlParseException("Invalid input at top level: '$c'")
            val tagStart = pos
            pos++
            when (peek()) {
                '?' -> { pos++; skipPI() }
                '!' -> {
                    pos++
                    when (peek()) {
                        'D' -> { pos++; skipDocType() }
                        '-' -> { pos++; skipComment() }
                        else -> skipBogusDecl()
                    }
                }
                else -> return parseElement(tagStart)
            }
        }
        throw XmlParseException("No root element")
    }

    private fun parseElement(start: Int): RawNode {
        val name = scanIdentifier()
        skipSpaces()
        val attrs = LinkedHashMap<String, String>()
        val attrOffsets = LinkedHashMap<String, IntRange>()
        val selfClosing = parseAttributes(attrs, attrOffsets)
        if (selfClosing) {
            return RawNode(name, attrs, attrOffsets, emptyList(), null, start, pos, NO, NO)
        }

        val children = ArrayList<RawNode>()
        parseContent(name, children)
        val end = pos

        if (children.size == 1 && children[0].name == null) {
            val text = children[0]
            return RawNode(name, attrs, attrOffsets, emptyList(), text.content, start, end, text.start, text.end)
        }
        return RawNode(name, attrs, attrOffsets, children, null, start, end, NO, NO)
    }

    private fun parseAttributes(attrs: MutableMap<String, String>, attrOffsets: MutableMap<String, IntRange>): Boolean {
        while (true) {
            skipSpaces()
            when (peek()) {
                '>' -> { pos++; return false }
                '/' -> { pos++; expect('>'); return true }
            }
            val key = scanIdentifier()
            if (key.isEmpty()) throw XmlParseException("Expected attribute name at $pos")
            skipSpaces()
            expect('=')
            skipSpaces()
            val valueStart = pos + 1
            val value = scanString()
            val valueEnd = pos - 1
            if (!key.startsWith("xmlns")) {
                if (attrs.containsKey(key)) throw XmlParseException("Duplicate attribute: $key")
                attrs[key] = value
                attrOffsets[key] = valueStart until valueEnd
            }
        }
    }

    private fun parseContent(name: String, children: MutableList<RawNode>) {
        while (true) {
            val ws = StringBuilder()
            skipSpacesInto(ws)
            if (pos >= len) throw XmlParseException("Unexpected EOF in <$name>")

            if (s[pos] == '<') {
                if (handleTag(name, children)) return
                continue
            }

            val sb = StringBuilder(ws)
            var textStart = pos
            if (s[pos] == '&') {
                val end = findRefEnd()
                val resolved = if (end >= 0) resolveRef(s.substring(pos, end + 1)) else null
                if (resolved != null) {
                    sb.append(resolved)
                    val numeric = s[pos + 1] == '#'
                    pos = end + 1
                    if (!numeric) textStart = pos
                }
            }
            while (pos < len && s[pos] != '<') {
                if (s[pos] == '&') appendRef(sb) else { sb.append(s[pos]); pos++ }
            }
            addText(children, sb.toString(), textStart, pos)
        }
    }

    private fun handleTag(name: String, children: MutableList<RawNode>): Boolean {
        val tagStart = pos
        pos++
        when (peek()) {
            '/' -> {
                pos++
                skipSpaces()
                val closeName = scanIdentifier()
                if (closeName != name) throw XmlParseException("Closing tag does not match: `$closeName' != `$name'")
                skipSpaces()
                expect('>')
                return true
            }
            '?' -> { pos++; skipPI() }
            '!' -> {
                pos++
                when (peek()) {
                    '[' -> { pos++; readCData(children) }
                    'D' -> { pos++; skipDocType() }
                    '-' -> { pos++; skipComment() }
                    else -> skipBogusDecl()
                }
            }
            else -> children.add(parseElement(tagStart))
        }
        return false
    }

    private fun addText(children: MutableList<RawNode>, content: String, start: Int, end: Int) {
        val last = children.lastOrNull()
        if (last != null && last.name == null) {
            last.content = (last.content ?: "") + content
        } else {
            children.add(RawNode(null, emptyMap(), emptyMap(), emptyList(), content, start, end, NO, NO))
        }
    }

    private fun readCData(children: MutableList<RawNode>) {
        expectLiteral("CDATA[")
        val start = pos
        val idx = s.indexOf("]]>", pos)
        if (idx < 0) throw XmlParseException("Unterminated CDATA section")
        val content = s.substring(pos, idx)
        pos = idx + 3
        addText(children, content, start, pos)
    }

    private fun skipPI() {
        val idx = s.indexOf("?>", pos)
        if (idx < 0) throw XmlParseException("Unterminated processing instruction")
        pos = idx + 2
    }

    private fun skipComment() {
        expect('-')
        var dashes = 0
        while (pos < len) {
            val ch = s[pos]; pos++
            when {
                ch == '-' -> dashes++
                ch == '>' && dashes == 2 -> return
                else -> dashes = 0
            }
        }
        throw XmlParseException("Unterminated comment")
    }

    private fun skipDocType() {
        expectLiteral("OCTYPE")
        var depth = 0
        while (pos < len) {
            val ch = s[pos++]
            when {
                depth == 0 && (ch == '"' || ch == '\'') -> { while (pos < len && s[pos] != ch) pos++; if (pos < len) pos++ }
                ch == '[' -> depth++
                ch == ']' -> if (depth > 0) depth--
                ch == '>' && depth <= 0 -> return
            }
        }
        throw XmlParseException("Unterminated DOCTYPE")
    }

    private fun skipBogusDecl() {
        val idx = s.indexOf('>', pos)
        pos = if (idx < 0) len else idx + 1
    }

    private fun scanString(): String {
        val delim = peek()
        if (delim != '"' && delim != '\'') throw XmlParseException("Expected a delimited string at $pos")
        pos++
        val sb = StringBuilder()
        while (true) {
            if (pos >= len) throw XmlParseException("Unterminated attribute value")
            val c = s[pos]
            when {
                c == '&' -> appendRef(sb)
                c == delim -> { pos++; return sb.toString() }
                c == '\t' || c == '\n' || c == '\r' -> { sb.append(' '); pos++ }
                else -> { sb.append(c); pos++ }
            }
        }
    }

    private fun appendRef(sb: StringBuilder) {
        val end = findRefEnd()
        val resolved = if (end >= 0) resolveRef(s.substring(pos, end + 1)) else null
        if (resolved == null) {
            sb.append('&'); pos++
        } else {
            sb.append(resolved); pos = end + 1
        }
    }

    private fun findRefEnd(): Int {
        var p = pos + 1
        while (p < len) {
            val c = s[p]
            if (c == ';') return p
            if (!(c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '#')) return -1
            p++
        }
        return -1
    }

    private fun resolveRef(ref: String): Char? =
        if (ref.startsWith("&#")) runCatching { decodeCharRef(ref) }.getOrNull() else Entities.lookup(ref)

    private fun scanIdentifier(): String {
        val start = pos
        while (pos < len) {
            val ch = s[pos]
            val ok = ch == '_' || ch == ':' || ch == '-' || ch == '.' ||
                ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9' || ch > '~'
            if (!ok) break
            pos++
        }
        return s.substring(start, pos)
    }

    private fun skipSpaces() {
        while (pos < len) {
            val ch = s[pos]
            if (ch != ' ' && ch != '\t' && ch != '\n') break
            pos++
        }
    }

    private fun skipSpacesInto(buffer: StringBuilder) {
        while (pos < len) {
            val ch = s[pos]
            if (ch != ' ' && ch != '\t' && ch != '\n') break
            buffer.append(if (ch == '\n') '\n' else ' ')
            pos++
        }
    }

    private fun peek(): Char = if (pos < len) s[pos] else ' '

    private fun expect(ch: Char) {
        if (pos >= len || s[pos] != ch) throw XmlParseException("Expected `$ch' at $pos")
        pos++
    }

    private fun expectLiteral(literal: String) {
        if (!s.startsWith(literal, pos)) throw XmlParseException("Expected `$literal' at $pos")
        pos += literal.length
    }
}

internal fun decodeCharRef(ref: String): Char {
    val body = ref.substring(2, ref.length - 1)
    return if (body.startsWith("x")) body.substring(1).toInt(16).toChar() else body.toInt().toChar()
}

internal object Entities {
    private val table: Map<String, Char> = mapOf(
        // Default XML entities.
        "amp" to "&#38;", "quot" to "&#34;", "apos" to "&#39;", "lt" to "&#60;", "gt" to "&#62;",
        // Mac symbols.
        "Command" to "&#x2318;", "Option" to "&#x2325;", "Control" to "&#x2303;", "Shift" to "&#x21E7;",
        "CapsLock" to "&#x21EA;", "Return" to "&#x21A9;", "Enter" to "&#x2324;", "Delete" to "&#x232B;",
        "ForwardDelete" to "&#x2326;", "Escape" to "&#x238B;", "MenuItem" to "&#x25B8;", "Eject" to "&#x23CF;",
        "Power" to "&#x233D;", "Tab" to "&#x21E5;", "PageUp" to "&#x21DE;", "PageDown" to "&#x21DF;",
        "Home" to "&#x2196;", "End" to "&#x2198;", "LeftArrow" to "&#x2190;", "RightArrow" to "&#x2192;",
        "UpArrow" to "&#x2191;", "DownArrow" to "&#x2193;",
        // Special characters.
        "percnt" to "&#37;", "iexcl" to "&#161;", "cent" to "&#162;", "pound" to "&#163;",
        "curren" to "&#164;", "yen" to "&#165;", "brvbar" to "&#166;", "sect" to "&#167;",
        "uml" to "&#168;", "copy" to "&#169;", "ordf" to "&#170;", "laquo" to "&#171;",
        "ldquo" to "&#171;", "not" to "&#172;", "shy" to "&#173;", "reg" to "&#174;",
        "macr" to "&#175;", "deg" to "&#176;", "plusmn" to "&#177;", "sup2" to "&#178;",
        "sup3" to "&#179;", "acute" to "&#180;", "micro" to "&#181;", "para" to "&#182;",
        "middot" to "&#183;", "cedil" to "&#184;", "sup" to "&#185;", "ordm" to "&#186;",
        "raquo" to "&#187;", "rdquo" to "&#187;", "frac14" to "&#188;", "frac12" to "&#189;",
        "frac34" to "&#190;", "iquest" to "&#191;", "times" to "&#215;", "divide" to "&#247;",
        // Latin characters.
        "Agrave" to "&#192;", "Aacute" to "&#193;", "Acirc" to "&#194;", "Atilde" to "&#195;",
        "Auml" to "&#196;", "Aring" to "&#197;", "AElig" to "&#198;", "Ccedil" to "&#199;",
        "Egrave" to "&#200;", "Eacute" to "&#201;", "Ecirc" to "&#202;", "Euml" to "&#203;",
        "Igrave" to "&#204;", "Iacute" to "&#205;", "Icirc" to "&#206;", "Iuml" to "&#207;",
        "ETH" to "&#208;", "Ntilde" to "&#209;", "Ograve" to "&#210;", "Oacute" to "&#211;",
        "Ocirc" to "&#212;", "Otilde" to "&#213;", "Ouml" to "&#214;", "Oslash" to "&#216;",
        "Ugrave" to "&#217;", "Uacute" to "&#218;", "Ucirc" to "&#219;", "Uuml" to "&#220;",
        "Yacute" to "&#221;", "THORN" to "&#222;", "szlig" to "&#223;", "agrave" to "&#224;",
        "aacute" to "&#225;", "acirc" to "&#226;", "atilde" to "&#227;", "auml" to "&#228;",
        "aring" to "&#229;", "aelig" to "&#230;", "ccedil" to "&#231;", "egrave" to "&#232;",
        "eacute" to "&#233;", "ecirc" to "&#234;", "euml" to "&#235;", "igrave" to "&#236;",
        "iacute" to "&#237;", "icirc" to "&#238;", "iuml" to "&#239;", "eth" to "&#240;",
        "ntilde" to "&#241;", "ograve" to "&#242;", "oacute" to "&#243;", "ocirc" to "&#244;",
        "otilde" to "&#245;", "ouml" to "&#246;", "oslash" to "&#248;", "ugrave" to "&#249;",
        "uacute" to "&#250;", "ucirc" to "&#251;", "uuml" to "&#252;", "yacute" to "&#253;",
        "thorn" to "&#254;", "yuml" to "&#255;", "nbsp" to "&#160;", "mdash" to "&#8212;",
        "ndash" to "&#8211;",
    ).mapValues { decodeCharRef(it.value) }

    fun lookup(ref: String): Char? = table[ref.substring(1, ref.length - 1)]
}