package finder.parsing

import finder.parsing.ParserType.*
import kotlin.test.Test
import kotlin.test.assertEquals

class FileMaskTest {

    @Test
    fun `explicit per-extension parsers`() {
        assertEquals(
            mapOf("topic" to XML, "md" to MARKDOWN),
            FileMask.resolve("topic:xml,md:md")
        )
    }

    @Test
    fun `extensions without a parser use their default`() {
        assertEquals(
            mapOf("xml" to XML, "md" to MARKDOWN),
            FileMask.resolve("xml,md")
        )
    }

    @Test
    fun `unknown extension without a parser falls back to the default parser`() {
        assertEquals(mapOf("txt" to FileMask.DEFAULT_PARSER), FileMask.resolve("txt"))
    }

    @Test
    fun `blank spec defaults to all extensions that have parsers`() {
        assertEquals(FileMask.defaultByExtension, FileMask.resolve(""))
    }

    @Test
    fun `unknown parser for an extension falls back to its default`() {
        assertEquals(mapOf("md" to MARKDOWN, "topic" to FILE), FileMask.resolve("md:bogus,topic:nope"))
    }

    @Test
    fun `wildcard maps any extension to a parser`() {
        assertEquals(mapOf("*" to LINE), FileMask.resolve("*:line"))
    }

    @Test
    fun `wildcard coexists with explicit extensions`() {
        assertEquals(mapOf("md" to MARKDOWN, "*" to LINE), FileMask.resolve("md,*:line"))
    }

    @Test
    fun `defaults description renders every default mapping as extension colon parser`() {
        assertEquals(
            "md:md, mdx:md, xml:xml, adoc:adoc, asciidoc:adoc, properties:properties",
            FileMask.defaultsDescription
        )
    }
}
