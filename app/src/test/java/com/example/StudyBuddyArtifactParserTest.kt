package com.example

import com.example.buddy.BuddyBlock
import com.example.buddy.StudyBuddyArtifactParser
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
class StudyBuddyArtifactParserTest {

    @Test
    fun testValidStructuredJsonParsing() {
        val jsonInput = """
            {
              "summary": "Here are 5 target Vector questions",
              "blocks": [
                {
                  "type": "insight",
                  "title": "HSC & Admission Practice",
                  "text": "5 practice problems cover vector dot and cross products.",
                  "level": "HIGHLIGHT"
                },
                {
                  "type": "quiz",
                  "question": "Two equal forces act at an angle theta. What is the resultant magnitude?",
                  "options": ["60°", "90°", "120°", "180°"],
                  "correctIndex": 2,
                  "explanation": "Use resultant formula R = sqrt(A^2 + B^2 + 2AB cos theta)."
                }
              ]
            }
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(jsonInput)
        assertEquals("Here are 5 target Vector questions", artifact.summary)
        assertEquals(2, artifact.blocks.size)

        val insight = artifact.blocks[0] as BuddyBlock.InsightBlock
        assertEquals("HSC & Admission Practice", insight.title)
        assertEquals("5 practice problems cover vector dot and cross products.", insight.text)

        val quiz = artifact.blocks[1] as BuddyBlock.QuizBlock
        assertEquals("Two equal forces act at an angle theta. What is the resultant magnitude?", quiz.question)
        assertEquals(4, quiz.options.size)
        assertEquals(2, quiz.correctIndex)
    }

    @Test
    fun testMarkdownCodeFencedJsonParsing() {
        val fencedInput = """
            ```json
            {
              "summary": "Chapter Summary",
              "blocks": [
                {
                  "type": "text",
                  "content": "Vectors have both magnitude and direction."
                }
              ]
            }
            ```
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(fencedInput)
        assertEquals("Chapter Summary", artifact.summary)
        assertEquals(1, artifact.blocks.size)
        assertTrue(artifact.blocks[0] is BuddyBlock.TextBlock)
        assertEquals("Vectors have both magnitude and direction.", (artifact.blocks[0] as BuddyBlock.TextBlock).content)
    }

    @Test
    fun testUnescapedLatexInJsonString() {
        val latexJson = """
            {
              "summary": "Vector Dot Product",
              "blocks": [
                {
                  "type": "text",
                  "content": "Formula: \vec{A} \cdot \vec{B} = |A||B|\cos\theta"
                }
              ]
            }
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(latexJson)
        assertEquals("Vector Dot Product", artifact.summary)
        assertEquals(1, artifact.blocks.size)
    }

    @Test
    fun testQuizCorrectIndexOutOfBoundsIsClamped() {
        val badIndexJson = """
            {
              "summary": "Quiz test",
              "blocks": [
                {
                  "type": "quiz",
                  "question": "What is the unit of force?",
                  "options": ["Joule", "Newton", "Watt"],
                  "correctIndex": 99,
                  "explanation": "Force is measured in Newtons."
                }
              ]
            }
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(badIndexJson)
        val quiz = artifact.blocks[0] as BuddyBlock.QuizBlock
        // Correct index 99 must be clamped to max option index 2
        assertEquals(2, quiz.correctIndex)
    }

    @Test
    fun testStepByStepBlockParsing() {
        val jsonInput = """
            {
              "summary": "Step-by-step resolution",
              "blocks": [
                {
                  "type": "step_by_step",
                  "title": "Projectile Maximum Height",
                  "steps": [
                    {
                      "stepNumber": 1,
                      "title": "Step 1",
                      "content": "Identify vertical velocity component v_{0y} = v_0 \sin\theta",
                      "isCrucial": false
                    },
                    {
                      "stepNumber": 2,
                      "title": "Step 2",
                      "content": "Apply kinematic equation at peak v_y^2 = v_{0y}^2 - 2gH",
                      "isCrucial": true
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(jsonInput)
        assertEquals(1, artifact.blocks.size)
        val stepBlock = artifact.blocks[0] as BuddyBlock.StepByStepBlock
        assertEquals("Projectile Maximum Height", stepBlock.title)
        assertEquals(2, stepBlock.steps.size)
        assertEquals(1, stepBlock.steps[0].stepNumber)
        assertFalse(stepBlock.steps[0].isCrucial)
        assertEquals(2, stepBlock.steps[1].stepNumber)
        assertTrue(stepBlock.steps[1].isCrucial)
    }

    @Test
    fun testFormulaBlockParsing() {
        val jsonInput = """
            {
              "summary": "Key Formula",
              "blocks": [
                {
                  "type": "formula_card",
                  "title": "Mass-Energy Equivalence",
                  "formulaLatex": "E = mc^2",
                  "variables": [
                    { "symbol": "E", "name": "Energy", "unit": "Joules (J)" },
                    { "symbol": "m", "name": "Mass", "unit": "kg" },
                    { "symbol": "c", "name": "Speed of light", "unit": "3 x 10^8 m/s" }
                  ],
                  "example": "Converting 1 gram yields 9 x 10^13 Joules."
                }
              ]
            }
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(jsonInput)
        assertEquals(1, artifact.blocks.size)
        val formulaBlock = artifact.blocks[0] as BuddyBlock.FormulaBlock
        assertEquals("E = mc^2", formulaBlock.formulaLatex)
        assertEquals("Mass-Energy Equivalence", formulaBlock.title)
        assertEquals(3, formulaBlock.variables.size)
        assertEquals("Speed of light", formulaBlock.variables[2].name)
        assertEquals("Converting 1 gram yields 9 x 10^13 Joules.", formulaBlock.example)
    }

    @Test
    fun testFlashcardBlockParsing() {
        val jsonInput = """
            {
              "summary": "Active Recall Flashcard",
              "blocks": [
                {
                  "type": "flashcard",
                  "front": "What is the First Law of Thermodynamics?",
                  "back": "Energy cannot be created or destroyed, only transformed: dQ = dU + dW.",
                  "hint": "Conservation of energy in thermal systems",
                  "tag": "Physics 2nd Paper"
                }
              ]
            }
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(jsonInput)
        assertEquals(1, artifact.blocks.size)
        val flashcard = artifact.blocks[0] as BuddyBlock.FlashcardBlock
        assertEquals("What is the First Law of Thermodynamics?", flashcard.front)
        assertTrue(flashcard.back.contains("dQ = dU + dW"))
        assertEquals("Conservation of energy in thermal systems", flashcard.hint)
    }

    @Test
    fun testConceptMapBlockParsing() {
        val jsonInput = """
            {
              "summary": "Organic Chemistry Map",
              "blocks": [
                {
                  "type": "concept_map",
                  "topic": "Hydrocarbons Breakdown",
                  "nodes": [
                    { "name": "Hydrocarbons", "description": "Organic compounds containing C and H only", "level": 0 },
                    { "name": "Aliphatic", "description": "Open-chain or non-aromatic rings", "level": 1 },
                    { "name": "Alkanes", "description": "Saturated single bonds", "level": 2 },
                    { "name": "Aromatic", "description": "Planar conjugated rings", "level": 1 }
                  ],
                  "connections": ["Hydrocarbons -> Aliphatic", "Aliphatic -> Alkanes"]
                }
              ]
            }
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(jsonInput)
        assertEquals(1, artifact.blocks.size)
        val map = artifact.blocks[0] as BuddyBlock.ConceptMapBlock
        assertEquals("Hydrocarbons Breakdown", map.topic)
        assertEquals(4, map.nodes.size)
        assertEquals("Hydrocarbons", map.nodes[0].name)
        assertEquals(2, map.connections.size)
    }

    @Test
    fun testWarningBlockParsing() {
        val jsonInput = """
            {
              "summary": "Common Exam Pitfall",
              "blocks": [
                {
                  "type": "warning",
                  "title": "Do not confuse Sine and Cosine in Vector Resolution!",
                  "cautionText": "Students often assume horizontal is always cos theta.",
                  "commonMistake": "R_x = R cos theta when theta is given with the vertical axis.",
                  "correctApproach": "Check which axis theta touches. If theta touches vertical, R_y = R cos theta, R_x = R sin theta."
                }
              ]
            }
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(jsonInput)
        assertEquals(1, artifact.blocks.size)
        val warning = artifact.blocks[0] as BuddyBlock.WarningBlock
        assertEquals("Do not confuse Sine and Cosine in Vector Resolution!", warning.title)
        assertEquals("Students often assume horizontal is always cos theta.", warning.cautionText)
        assertTrue(warning.correctApproach?.contains("Check which axis theta touches") == true)
    }

    @Test
    fun testUnparseableJsonIsSanitizedWithoutRawJsonSyntax() {
        val brokenJson = """
          {
            "summary": "Broken syntax",
            "blocks": [
              {"type": "insight", "text": unquoted broken text without quotes...
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(brokenJson)
        assertNotNull(artifact)
        // Verify output content does NOT contain raw JSON syntax keys like "summary": or "blocks":
        artifact.blocks.forEach { block ->
            if (block is BuddyBlock.TextBlock) {
                assertFalse(block.content.contains("\"summary\":"))
                assertFalse(block.content.contains("\"blocks\":"))
                assertFalse(block.content.contains("{\"type\":"))
            }
        }
    }
}
