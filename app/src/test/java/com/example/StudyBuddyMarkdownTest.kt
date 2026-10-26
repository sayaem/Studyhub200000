package com.example

import com.example.ui.screens.buddy.MarkdownBlock
import com.example.ui.screens.buddy.StudyBuddyMarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyBuddyMarkdownTest {

    @Test
    fun testHeadingsParsing() {
        val markdown = """
            # Priority Review Plan
            ## Recommended Subjects
            ### Physics Chapter 1
        """.trimIndent()

        val blocks = StudyBuddyMarkdownParser.parse(markdown)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading && (blocks[0] as MarkdownBlock.Heading).level == 1)
        assertTrue(blocks[1] is MarkdownBlock.Heading && (blocks[1] as MarkdownBlock.Heading).level == 2)
        assertTrue(blocks[2] is MarkdownBlock.Heading && (blocks[2] as MarkdownBlock.Heading).level == 3)
    }

    @Test
    fun testLiteralMarkdownBoldHeaderDetection() {
        val text = """
            **Revisions Due Today:**
            * Vectors - Physics (Due today)
            * Thermodynamics - Chemistry (Overdue)
            
            **Scheduled Tasks Today:**
            1. Revise Optics
            2. Complete Math Exercise
        """.trimIndent()

        val blocks = StudyBuddyMarkdownParser.parse(text)
        // First block should be recognized as a Heading (level 3) without literal asterisks
        assertTrue(blocks.any { it is MarkdownBlock.Heading && (it as MarkdownBlock.Heading).text == "Revisions Due Today" })
        // Bullet items should be extracted cleanly
        assertTrue(blocks.any { it is MarkdownBlock.BulletItem && (it as MarkdownBlock.BulletItem).text.contains("Vectors") })
        // Numbered items should be extracted cleanly
        assertTrue(blocks.any { it is MarkdownBlock.NumberedItem && (it as MarkdownBlock.NumberedItem).number == "1" })
    }

    @Test
    fun testInlineMarkdownAnnotatedString() {
        val sample = "Here is **bold text**, *italic text*, and `F = ma` with [StudyHub](https://example.com)."
        val annotated = StudyBuddyMarkdownParser.parseInlineMarkdown(sample)

        // The text must not contain raw Markdown syntax
        val rendered = annotated.text
        assertFalse(rendered.contains("**"))
        assertFalse(rendered.contains("`"))
        assertTrue(rendered.contains("bold text"))
        assertTrue(rendered.contains("italic text"))
        assertTrue(rendered.contains("F = ma"))
        assertTrue(rendered.contains("StudyHub"))
    }

    @Test
    fun testCodeBlockParsing() {
        val codeSample = """
            Here is the formula:
            ```physics
            F = G * (m1 * m2) / r^2
            ```
            Use this for gravitation.
        """.trimIndent()

        val blocks = StudyBuddyMarkdownParser.parse(codeSample)
        val codeBlock = blocks.filterIsInstance<MarkdownBlock.CodeBlock>().firstOrNull()
        assertNotNull(codeBlock)
        assertEquals("physics", codeBlock?.language)
        assertTrue(codeBlock?.code?.contains("m1 * m2") == true)
    }

    @Test
    fun testTableParsing() {
        val tableSample = """
            | Subject | Due Topics | Priority |
            |---|---|---|
            | Physics | 2 | High |
            | Chemistry | 1 | Medium |
        """.trimIndent()

        val blocks = StudyBuddyMarkdownParser.parse(tableSample)
        val tableBlock = blocks.filterIsInstance<MarkdownBlock.Table>().firstOrNull()
        assertNotNull(tableBlock)
        assertEquals(3, tableBlock?.headers?.size)
        assertEquals(2, tableBlock?.rows?.size)
    }

    @Test
    fun testMalformedMarkdownNeverCrashes() {
        val malformed = "This is broken **bold text without close and `code without close and [link](broken"
        val annotated = StudyBuddyMarkdownParser.parseInlineMarkdown(malformed)
        assertTrue(annotated.text.isNotEmpty())

        val blocks = StudyBuddyMarkdownParser.parse("```\nunclosed code block")
        assertTrue(blocks.isNotEmpty())
    }

    private fun assertFalse(condition: Boolean) {
        org.junit.Assert.assertFalse(condition)
    }
}
