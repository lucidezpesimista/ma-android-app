package com.ma.app

import com.ma.app.utils.TextParser
import org.junit.Assert.*
import org.junit.Test

class TextParserTest {

    @Test
    fun `extractHashtags finds all hashtags in text`() {
        val text = "Este es un #test con #multiple #tags123 y #tag_con_guion"
        val hashtags = TextParser.extractHashtags(text)

        assertEquals(4, hashtags.size)
        assertTrue(hashtags.contains("test"))
        assertTrue(hashtags.contains("multiple"))
        assertTrue(hashtags.contains("tags123"))
        assertTrue(hashtags.contains("tag_con_guion"))
    }

    @Test
    fun `extractHashtags returns empty list when no hashtags`() {
        val text = "Este texto no tiene hashtags"
        val hashtags = TextParser.extractHashtags(text)

        assertTrue(hashtags.isEmpty())
    }

    @Test
    fun `extractHashtags ignores invalid hashtags`() {
        val text = "#valid #123invalid #con-espacio #bien"
        val hashtags = TextParser.extractHashtags(text)

        assertEquals(2, hashtags.size)
        assertTrue(hashtags.contains("valid"))
        assertTrue(hashtags.contains("bien"))
    }

    @Test
    fun `extractInternalLinks finds all wiki links`() {
        val text = "Ver [[Nota Importante]] y tambien [[Otra Nota]]"
        val links = TextParser.extractInternalLinks(text)

        assertEquals(2, links.size)
        assertTrue(links.contains("Nota Importante"))
        assertTrue(links.contains("Otra Nota"))
    }

    @Test
    fun `extractInternalLinks returns empty list when no links`() {
        val text = "Texto sin links internos"
        val links = TextParser.extractInternalLinks(text)

        assertTrue(links.isEmpty())
    }

    @Test
    fun `parse returns both hashtags and links`() {
        val text = "#idea: revisar [[Documentacion]] y #implementar"
        val parsed = TextParser.parse(text)

        assertEquals(2, parsed.hashtags.size)
        assertTrue(parsed.hashtags.contains("idea"))
        assertTrue(parsed.hashtags.contains("implementar"))

        assertEquals(1, parsed.internalLinks.size)
        assertTrue(parsed.internalLinks.contains("Documentacion"))
    }

    @Test
    fun `containsHashtag checks correctly`() {
        val text = "Trabajar en #proyecto principal"

        assertTrue(TextParser.containsHashtag(text, "proyecto"))
        assertTrue(TextParser.containsHashtag(text, "#proyecto"))
        assertFalse(TextParser.containsHashtag(text, "otro"))
    }

    @Test
    fun `getTagRanges returns correct positions`() {
        val text = "Inicio #tag1 medio [[Link]] fin"
        val ranges = TextParser.getTagRanges(text)

        assertEquals(2, ranges.size)

        // Primer rango debe ser el hashtag
        assertEquals("tag1", ranges[0].tag)
        assertEquals(TextParser.TagType.HASHTAG, ranges[0].type)
        assertEquals(6, ranges[0].start)
        assertEquals(11, ranges[0].end)

        // Segundo rango debe ser el link
        assertEquals("Link", ranges[1].tag)
        assertEquals(TextParser.TagType.INTERNAL_LINK, ranges[1].type)
    }

    @Test
    fun `findTagAtPosition returns correct tag`() {
        val text = "Texto #importante aqui"

        val tag = TextParser.findTagAtPosition(text, 8) // Dentro de #importante
        assertNotNull(tag)
        assertEquals("importante", tag?.tag)
        assertEquals(TextParser.TagType.HASHTAG, tag?.type)

        val noTag = TextParser.findTagAtPosition(text, 3) // En "Texto"
        assertNull(noTag)
    }

    @Test
    fun `toInternalLink formats correctly`() {
        assertEquals("[[Mi Nota]]", TextParser.toInternalLink("Mi Nota"))
    }

    @Test
    fun `parse handles empty text`() {
        val parsed = TextParser.parse("")

        assertEquals(0, parsed.hashtags.size)
        assertEquals(0, parsed.internalLinks.size)
        assertEquals("", parsed.annotatedString.text)
    }

    @Test
    fun `extractHashtags removes duplicates`() {
        val text = "#test #test #otro #test"
        val hashtags = TextParser.extractHashtags(text)

        assertEquals(2, hashtags.size)
        assertTrue(hashtags.contains("test"))
        assertTrue(hashtags.contains("otro"))
    }
}
