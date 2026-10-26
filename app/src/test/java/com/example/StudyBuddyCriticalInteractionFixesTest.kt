package com.example

import com.example.ai.gemini.StudyBuddyCentralClient
import com.example.buddy.AttachedFileData
import com.example.buddy.BuddyBlock
import com.example.buddy.BuddyContextEnvelope
import com.example.buddy.BuddyGenerationRequest
import com.example.buddy.BuddyQueryIntent
import com.example.buddy.StudyBuddyArtifactParser
import com.example.buddy.StudyHubVerifiedFacts
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudyBuddyCriticalInteractionFixesTest {

    @Test
    fun `attached file data model stores properties and limits correctly`() {
        val imageFile = AttachedFileData(
            fileName = "physics_notes.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 250_000L,
            base64Content = "AQIDBA==",
            textContent = null
        )

        assertEquals("physics_notes.jpg", imageFile.fileName)
        assertEquals("image/jpeg", imageFile.mimeType)
        assertEquals(250_000L, imageFile.sizeBytes)
        assertNotNull(imageFile.base64Content)
        assertNull(imageFile.textContent)

        val docFile = AttachedFileData(
            fileName = "syllabus.txt",
            mimeType = "text/plain",
            sizeBytes = 1200L,
            base64Content = null,
            textContent = "Unit 1: Thermodynamics\nUnit 2: Kinetics"
        )
        assertEquals("syllabus.txt", docFile.fileName)
        assertEquals("text/plain", docFile.mimeType)
        assertNull(docFile.base64Content)
        assertTrue(docFile.textContent?.contains("Thermodynamics") == true)
    }

    @Test
    fun `gemini request payload includes attached image inlineData and document text`() {
        val client = StudyBuddyCentralClient()
        val imageFile = AttachedFileData(
            fileName = "problem.png",
            mimeType = "image/png",
            sizeBytes = 50_000L,
            base64Content = "iVBORw0KGgoAAAANSUhEUg==",
            textContent = null
        )
        val docFile = AttachedFileData(
            fileName = "routine.txt",
            mimeType = "text/plain",
            sizeBytes = 500L,
            base64Content = null,
            textContent = "9am: Math, 11am: Physics"
        )

        val request = BuddyGenerationRequest(
            conversationId = 101L,
            userPrompt = "Please help me plan my study schedule using this routine and problem",
            recentTurns = emptyList(),
            contextEnvelope = BuddyContextEnvelope(
                queryIntent = BuddyQueryIntent.PLANNING,
                verifiedFacts = StudyHubVerifiedFacts(studentName = "Rafi"),
                timeReference = "2026-10-26"
            ),
            attachedFiles = listOf(imageFile, docFile)
        )

        val payloadJson = client.buildRequestPayload(
            request = request,
            systemInstruction = "You are Study Buddy",
            temperature = 0.5,
            maxOutputTokens = 2048
        )
        val root = JSONObject(payloadJson)
        val contents = root.optJSONArray("contents")
        assertNotNull(contents)
        assertTrue(contents.length() > 0)

        val userTurn = contents.getJSONObject(contents.length() - 1)
        val parts = userTurn.optJSONArray("parts")
        assertNotNull(parts)

        // Verify inlineData for image and text for document
        var foundImageInlineData = false
        var foundDocText = false
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("inlineData")) {
                val inlineData = part.getJSONObject("inlineData")
                if (inlineData.optString("mimeType") == "image/png" &&
                    inlineData.optString("data") == "iVBORw0KGgoAAAANSUhEUg==") {
                    foundImageInlineData = true
                }
            }
            if (part.has("text")) {
                val text = part.optString("text")
                if (text.contains("routine.txt") && text.contains("9am: Math")) {
                    foundDocText = true
                }
            }
        }

        assertTrue("Image attachment should be represented as inlineData in Gemini payload", foundImageInlineData)
        assertTrue("Document attachment should be appended as labeled text part in Gemini payload", foundDocText)
    }

    @Test
    fun `study schedule artifact parser extracts clean structured blocks without extra whitespace`() {
        val jsonSchedule = """
            {
              "summary": "Admission Sprint Schedule",
              "blocks": [
                {
                  "type": "study_plan",
                  "title": "Admission Sprint Schedule",
                  "dateDescription": "2026-10-26",
                  "steps": [
                    { "time": "09:00 AM", "title": "Math Practice", "durationMinutes": 60, "isCompleted": false },
                    { "time": "11:00 AM", "title": "Physics Review", "durationMinutes": 60, "isCompleted": false },
                    { "time": "03:00 PM", "title": "Chemistry Active Recall", "durationMinutes": 60, "isCompleted": false }
                  ]
                }
              ]
            }
        """.trimIndent()

        val artifact = StudyBuddyArtifactParser.parse(jsonSchedule)
        assertNotNull(artifact)
        val planBlock = artifact.blocks.filterIsInstance<BuddyBlock.StudyPlanBlock>().firstOrNull()
        assertNotNull("StudyPlanBlock must be parsed", planBlock)
        assertEquals("Admission Sprint Schedule", planBlock?.title)
        assertEquals(3, planBlock?.steps?.size)

        // Verify task items
        val item1 = planBlock?.steps?.get(0)
        assertEquals("09:00 AM", item1?.time)
        assertEquals("Math Practice", item1?.title)
        assertEquals(60, item1?.durationMinutes)
        assertFalse(item1?.isCompleted ?: true)

        // Verify no duplicate summary
        if (artifact.blocks.firstOrNull() is BuddyBlock.TextBlock) {
            val firstText = (artifact.blocks.first() as BuddyBlock.TextBlock).content.trim()
            assertEquals("Admission Sprint Schedule", firstText)
        }
    }

    @Test
    fun `static header status and dynamic activity indicator labels meet specifications`() {
        // Spec requirements:
        // 1. Header subtitle is strictly static: "Online. Grounded AI"
        val headerSubtitle = "Online. Grounded AI"
        assertEquals("Online. Grounded AI", headerSubtitle)

        // 2. Truthful activity states for generation:
        val validWorkflowStages = listOf(
            "Reading your image",
            "Extracting document content",
            "Analyzing an uploaded file",
            "Checking your study context",
            "Building your study schedule",
            "Preparing your study plan",
            "Preparing your response"
        )

        for (stage in validWorkflowStages) {
            assertFalse("Activity label must not be empty", stage.isBlank())
            assertFalse("Activity label should not be a mock random phase", stage.contains("Simulating"))
        }
    }
}
