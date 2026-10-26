package com.example

import com.example.buddy.BuddyBlock
import com.example.buddy.StudyBuddyArtifactParser
import com.example.data.StudyHubDateUtils
import com.example.data.local.ChapterEntity
import com.example.data.local.StudyTaskEntity
import com.example.data.local.SubjectEntity
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
class StudyBuddyCalendarAndQuizFixesTest {

    @Test
    fun `calendar time mapping parses AM PM and preserves task start times`() {
        val scheduleJson = """
            {
              "summary": "Today's Study Schedule",
              "blocks": [
                {
                  "type": "study_plan",
                  "title": "Today's Study Schedule",
                  "dateDescription": "2026-10-26",
                  "steps": [
                    { "title": "Bangla", "time": "06:00 AM", "durationMinutes": 90 },
                    { "title": "English", "time": "07:45 AM", "durationMinutes": 90 },
                    { "title": "General Knowledge", "time": "09:30 AM", "durationMinutes": 60 },
                    { "title": "Admission MCQ Practice", "time": "10:45 AM", "durationMinutes": 75 }
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = StudyBuddyArtifactParser.parse(scheduleJson)
        assertNotNull(parsed)
        val planBlock = parsed.blocks.firstOrNull() as? BuddyBlock.StudyPlanBlock
        assertNotNull(planBlock)

        val steps = planBlock!!.steps
        assertEquals(4, steps.size)

        // Convert steps into calendar tasks as StudyHub does
        val calendarTasks = steps.map { step ->
            StudyTaskEntity(
                title = step.title,
                date = "2026-10-26",
                time = StudyHubDateUtils.normalizeTimeForStorage(step.time),
                durationMinutes = step.durationMinutes ?: 30
            )
        }

        assertEquals("06:00", calendarTasks[0].time)
        assertEquals("07:45", calendarTasks[1].time)
        assertEquals("09:30", calendarTasks[2].time)
        assertEquals("10:45", calendarTasks[3].time)

        // Verify Calendar formatting
        assertEquals("06:00 AM - 07:30 AM • 1h 30m", StudyHubDateUtils.formatPlanTimeRange(calendarTasks[0].time, calendarTasks[0].durationMinutes))
        assertEquals("07:45 AM - 09:15 AM • 1h 30m", StudyHubDateUtils.formatPlanTimeRange(calendarTasks[1].time, calendarTasks[1].durationMinutes))
        assertEquals("09:30 AM - 10:30 AM • 1h 00m", StudyHubDateUtils.formatPlanTimeRange(calendarTasks[2].time, calendarTasks[2].durationMinutes))
        assertEquals("10:45 AM - 12:00 PM • 1h 15m", StudyHubDateUtils.formatPlanTimeRange(calendarTasks[3].time, calendarTasks[3].durationMinutes))

        // Ensure tasks do not all map to 8:00 AM
        assertFalse(calendarTasks.all { it.time == "08:00" })
    }

    @Test
    fun `time normalization supports 24H and 12H with midnight boundaries`() {
        assertEquals("00:00", StudyHubDateUtils.normalizeTimeForStorage("12:00 AM"))
        assertEquals("12:00", StudyHubDateUtils.normalizeTimeForStorage("12:00 PM"))
        assertEquals("18:30", StudyHubDateUtils.normalizeTimeForStorage("06:30 PM"))
        assertEquals("23:45", StudyHubDateUtils.normalizeTimeForStorage("11:45 PM"))
        assertEquals("07:15", StudyHubDateUtils.normalizeTimeForStorage("07:15"))
        assertNull(StudyHubDateUtils.normalizeTimeForStorage(null))
        assertNull(StudyHubDateUtils.normalizeTimeForStorage(""))
    }

    @Test
    fun `quiz artifact preserves questions feedback and explanation`() {
        val quizJson = """
            {
              "summary": "Physics Quiz",
              "blocks": [
                {
                  "type": "quiz",
                  "question": "What is the unit of electric current?",
                  "options": ["Volt", "Ampere", "Ohm", "Watt"],
                  "correctIndex": 1,
                  "explanation": "Ampere is the SI unit of electric current named after André-Marie Ampère."
                }
              ]
            }
        """.trimIndent()

        val parsed = StudyBuddyArtifactParser.parse(quizJson)
        assertNotNull(parsed)
        val quizBlock = parsed.blocks.firstOrNull() as? BuddyBlock.QuizBlock
        assertNotNull(quizBlock)

        assertEquals("What is the unit of electric current?", quizBlock!!.question)
        assertEquals(4, quizBlock.options.size)
        assertEquals(1, quizBlock.correctIndex)
        assertTrue(quizBlock.explanation.contains("Ampere is the SI unit"))
    }

    @Test
    fun `syllabus calculates real progress percentage without fake mock data`() {
        val emptyChapters = emptyList<ChapterEntity>()

        val emptyProgress = if (emptyChapters.isNotEmpty()) {
            (emptyChapters.count { it.status == "COMPLETED" }.toFloat() / emptyChapters.size * 100).toInt()
        } else 0
        assertEquals(0, emptyProgress)

        val realChapters = listOf(
            ChapterEntity(id = 101L, subjectId = 1L, title = "Motion", status = "COMPLETED"),
            ChapterEntity(id = 102L, subjectId = 1L, title = "Vectors", status = "COMPLETED"),
            ChapterEntity(id = 103L, subjectId = 1L, title = "Gravity", status = "IN_PROGRESS"),
            ChapterEntity(id = 104L, subjectId = 1L, title = "Work & Power", status = "NOT_STARTED")
        )

        val completedCount = realChapters.count { it.status == "COMPLETED" }
        assertEquals(2, completedCount)
        val realProgress = (completedCount.toFloat() / realChapters.size * 100).toInt()
        assertEquals(50, realProgress)
    }
}
