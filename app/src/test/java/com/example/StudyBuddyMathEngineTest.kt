package com.example

import com.example.buddy.BuddyArtifactResponse
import com.example.buddy.BuddyBlock
import com.example.buddy.StudyBuddyArtifactParser
import com.example.ui.screens.buddy.MarkdownBlock
import com.example.ui.screens.buddy.StudyBuddyMarkdownParser
import com.example.ui.screens.buddy.StudyBuddyMathEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudyBuddyMathEngineTest {

    @Test
    fun testVectorDotProductFormat() {
        val latex = """\vec{A} \cdot \vec{B} = |A||B| \cos\theta"""
        val result = StudyBuddyMathEngine.formatLatexToUnicode(latex)
        // Check that vectors, dot product, and cos theta are properly converted
        assertTrue(result.contains("A⃗"))
        assertTrue(result.contains("B⃗"))
        assertTrue(result.contains("·"))
        assertTrue(result.contains("θ"))
        assertFalse(result.contains("\\vec"))
        assertFalse(result.contains("\\cdot"))
        assertFalse(result.contains("\\cos"))
        assertFalse(result.contains("\\theta"))
    }

    @Test
    fun testFractionsAndRoots() {
        val latex = """\frac{-b \pm \sqrt{b^2 - 4ac}}{2a}"""
        val result = StudyBuddyMathEngine.formatLatexToUnicode(latex)
        assertTrue(result.contains("±"))
        assertTrue(result.contains("√"))
        assertTrue(result.contains("b²"))
        assertTrue(result.contains("2a"))
        assertFalse(result.contains("\\frac"))
        assertFalse(result.contains("\\sqrt"))
        assertFalse(result.contains("\\pm"))
    }

    @Test
    fun testGreekLetters() {
        val latex = """\alpha + \beta = \gamma \cdot \lambda \times \omega"""
        val result = StudyBuddyMathEngine.formatLatexToUnicode(latex)
        assertTrue(result.contains("α"))
        assertTrue(result.contains("β"))
        assertTrue(result.contains("γ"))
        assertTrue(result.contains("λ"))
        assertTrue(result.contains("ω"))
        assertTrue(result.contains("·"))
        assertTrue(result.contains("×"))
    }

    @Test
    fun testSubscriptsAndSuperscripts() {
        val latex = """E_k = \frac{1}{2} m v^2"""
        val result = StudyBuddyMathEngine.formatLatexToUnicode(latex)
        assertTrue(result.contains("Eₖ"))
        assertTrue(result.contains("v²"))
        assertTrue(result.contains("1 / 2") || result.contains("1/2"))
    }

    @Test
    fun testDisplayMathBlockExtraction() {
        val markdownWithDisplayMath = """
            Here is Newton's Second Law:
            $$ \vec{F} = m \vec{a} $$
            And here is mass-energy equivalence:
            \[ E = mc^2 \]
            Remember this well!
        """.trimIndent()

        val blocks = StudyBuddyMarkdownParser.parse(markdownWithDisplayMath)
        val mathBlocks = blocks.filterIsInstance<MarkdownBlock.MathDisplay>()
        assertEquals(2, mathBlocks.size)
        assertTrue(mathBlocks[0].mathContent.contains("\\vec{F} = m \\vec{a}"))
        assertTrue(mathBlocks[1].mathContent.contains("E = mc^2"))
    }

    @Test
    fun testInlineMathInParagraph() {
        val text = "The work done is given by \\(W = \\vec{F} \\cdot \\vec{d}\\) in Joules."
        val annotated = StudyBuddyMarkdownParser.parseInlineMarkdown(text)
        val rendered = annotated.text
        assertFalse(rendered.contains("\\("))
        assertFalse(rendered.contains("\\)"))
        assertFalse(rendered.contains("\\vec"))
        assertFalse(rendered.contains("\\cdot"))
        assertTrue(rendered.contains("W = F⃗ · d⃗"))
    }

    @Test
    fun testDollarInlineMath() {
        val text = "Calculate ${'$'}a^2 + b^2 = c^2${'$'} for a right-angled triangle."
        val annotated = StudyBuddyMarkdownParser.parseInlineMarkdown(text)
        val rendered = annotated.text
        assertTrue(rendered.contains("a² + b² = c²"))
    }

    @Test
    fun testCurrencyDollarNotMangled() {
        val text = "The registration fee is \$50 per student, and the lab book costs \$15."
        val annotated = StudyBuddyMarkdownParser.parseInlineMarkdown(text)
        val rendered = annotated.text
        assertTrue(rendered.contains("\$50"))
        assertTrue(rendered.contains("\$15"))
    }

    @Test
    fun testCodeBlockWithBackslashesNotTreatedAsMath() {
        val markdown = """
            ```kotlin
            val path = "C:\\Users\\Student\\Documents"
            val price = "${'$'}100"
            ```
        """.trimIndent()
        val blocks = StudyBuddyMarkdownParser.parse(markdown)
        val codeBlock = blocks.filterIsInstance<MarkdownBlock.CodeBlock>().firstOrNull()
        assertNotNull(codeBlock)
        assertTrue(codeBlock?.code?.contains("""C:\\Users\\Student\\Documents""") == true)
        assertTrue(codeBlock?.code?.contains("${'$'}100") == true)
    }

    @Test
    fun testBanglaWithEnglishMath() {
        val mixed = "ভেক্টর ডট গুণনের সূত্র হলো \\(\\vec{A} \\cdot \\vec{B} = |A||B| \\cos\\theta\\)। এটি মনে রাখবে।"
        val annotated = StudyBuddyMarkdownParser.parseInlineMarkdown(mixed)
        val rendered = annotated.text
        assertTrue(rendered.contains("ভেক্টর ডট গুণনের সূত্র হলো"))
        assertTrue(rendered.contains("A⃗ · B⃗ = |A||B|"))
        assertTrue(rendered.contains("cosθ") || rendered.contains("cos θ") || rendered.contains("cos(θ)"))
        assertTrue(rendered.contains("এটি মনে রাখবে।"))
        assertFalse(rendered.contains("\\vec"))
    }

    @Test
    fun testMalformedLatexNeverCrashes() {
        val malformed = "\\( \\frac{1}{2 \\sqrt{broken without closing"
        val result = StudyBuddyMathEngine.formatLatexToUnicode(malformed)
        assertTrue(result.isNotEmpty())
        val annotated = StudyBuddyMarkdownParser.parseInlineMarkdown(malformed)
        assertTrue(annotated.text.isNotEmpty())
    }

    @Test
    fun testQuizBlockWithMathFormulas() {
        val jsonPayload = """
            {
                "summary": "Here is a quick question on vectors:",
                "blocks": [
                    {
                        "type": "quiz",
                        "question": "If \\(\\vec{A} = 3\\hat{i} + 4\\hat{j}\\), what is the magnitude \\(|\\vec{A}|\\)?",
                        "options": ["3", "4", "5", "7"],
                        "correctIndex": 2,
                        "explanation": "Magnitude is \\(\\sqrt{3^2 + 4^2} = \\sqrt{9 + 16} = 5\\)."
                    }
                ]
            }
        """.trimIndent()

        val parsed = StudyBuddyArtifactParser.parse(jsonPayload)
        val quiz = parsed.blocks.filterIsInstance<BuddyBlock.QuizBlock>().firstOrNull()
        assertNotNull(quiz)
        assertEquals(2, quiz?.correctIndex)

        // Verify that question and explanation are formatted with math engine
        val questionFormatted = StudyBuddyMarkdownParser.parseInlineMarkdown(quiz!!.question).text
        assertTrue(questionFormatted.contains("A⃗ = 3î + 4ĵ"))
        assertTrue(questionFormatted.contains("|A⃗|"))
        assertFalse(questionFormatted.contains("\\vec"))
        assertFalse(questionFormatted.contains("\\hat"))

        val explanationFormatted = StudyBuddyMarkdownParser.parseInlineMarkdown(quiz.explanation).text
        assertTrue(explanationFormatted.contains("√(3² + 4²)"))
        assertTrue(explanationFormatted.contains("= 5"))
    }
}
