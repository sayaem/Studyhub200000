package com.example

import com.example.buddy.BuddyActionType
import com.example.buddy.BuddyContextCategory
import com.example.buddy.BuddyContextEnvelope
import com.example.buddy.BuddyQueryIntent
import com.example.buddy.StudyBuddyContextEngine
import com.example.buddy.StudyHubVerifiedFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyBuddyContextEngineTest {

    @Test
    fun testIntentClassification() {
        assertEquals(BuddyQueryIntent.PLANNING, StudyBuddyContextEngine.classifyIntent("Help me plan my study schedule for today"))
        assertEquals(BuddyQueryIntent.PLANNING, StudyBuddyContextEngine.classifyIntent("I missed my study routine"))
        assertEquals(BuddyQueryIntent.REVISION, StudyBuddyContextEngine.classifyIntent("What topics should I revise today with active recall?"))
        assertEquals(BuddyQueryIntent.EXAM_PREPARATION, StudyBuddyContextEngine.classifyIntent("When is my next BUET admission test?"))
        assertEquals(BuddyQueryIntent.STATS_PROGRESS, StudyBuddyContextEngine.classifyIntent("How many hours have I logged and what is my progress?"))
        assertEquals(BuddyQueryIntent.FOCUS_HABITS, StudyBuddyContextEngine.classifyIntent("Start pomodoro timer and avoid distractions"))
        assertEquals(BuddyQueryIntent.TOPIC_SYLLABUS, StudyBuddyContextEngine.classifyIntent("Explain physics Chapter 4 thermodynamics"))
        assertEquals(BuddyQueryIntent.GENERAL, StudyBuddyContextEngine.classifyIntent("How do I stay motivated when feeling exhausted?"))
    }

    @Test
    fun testTruthfulnessInEmptyFacts() {
        val emptyFacts = StudyHubVerifiedFacts(
            studentName = "Rahim",
            academicLevel = "HSC",
            admissionTrack = "Engineering",
            dailyGoalHours = 4.0f,
            todayPlannedTasksCount = 0,
            todayPlannedTasksSummary = emptyList(),
            totalSubjectsCount = 6,
            totalChaptersCount = 42,
            completedChaptersCount = 10,
            incompleteChaptersCount = 32,
            dueRevisionsCount = 0,
            dueRevisionSubjects = emptyList(),
            todayStudyMinutesRecorded = 0,
            totalStudyHoursRecorded = 0f,
            nextExamTarget = null,
            specificTopicName = null
        )

        val envelope = BuddyContextEnvelope(
            queryIntent = BuddyQueryIntent.PLANNING,
            verifiedFacts = emptyFacts,
            timeReference = "Sunday, 25 October 2026, 05:00 PM"
        )

        val instruction = StudyBuddyContextEngine.buildSystemInstruction(envelope)

        // Verify truthful acknowledging of zero/empty state
        assertTrue(instruction.contains("(No tasks scheduled for today)"))
        assertTrue(instruction.contains("No upcoming exam date recorded."))
        assertTrue(instruction.contains("0 items due"))
        assertTrue(instruction.contains("10/42 chapters completed"))
        assertTrue(instruction.contains("Rahim"))
    }

    @Test
    fun testTruthfulnessWithPopulatedFacts() {
        val facts = StudyHubVerifiedFacts(
            studentName = "Anika",
            academicLevel = "HSC 2026",
            admissionTrack = "Medical",
            dailyGoalHours = 6.0f,
            todayPlannedTasksCount = 2,
            todayPlannedTasksSummary = listOf("Revise Botany Chapter 2 (PENDING)", "Solve Chemistry MCQ (COMPLETED)"),
            totalSubjectsCount = 5,
            totalChaptersCount = 35,
            completedChaptersCount = 20,
            incompleteChaptersCount = 15,
            dueRevisionsCount = 3,
            dueRevisionSubjects = listOf("Physics", "Biology"),
            todayStudyMinutesRecorded = 90,
            totalStudyHoursRecorded = 45.5f,
            nextExamTarget = "DMC (Medical) on 2026-11-20",
            specificTopicName = "Cell Division",
            specificSubjectName = "Biology"
        )

        val envelope = BuddyContextEnvelope(
            queryIntent = BuddyQueryIntent.REVISION,
            verifiedFacts = facts,
            timeReference = "Sunday, 25 October 2026, 05:00 PM"
        )

        val instruction = StudyBuddyContextEngine.buildSystemInstruction(envelope)

        assertTrue(instruction.contains("Anika"))
        assertTrue(instruction.contains("3 items due"))
        assertTrue(instruction.contains("Physics, Biology"))
        assertTrue(instruction.contains("DMC (Medical) on 2026-11-20"))
        assertTrue(instruction.contains("Revise Botany Chapter 2 (PENDING)"))
        assertTrue(instruction.contains("Cell Division"))
    }

    @Test
    fun testDisabledContextCategoriesAreHonored() {
        val facts = StudyHubVerifiedFacts(
            studentName = "Tanvir",
            dueRevisionsCount = 5,
            dueRevisionSubjects = listOf("Math", "Physics"),
            totalSubjectsCount = 4,
            completedChaptersCount = 12,
            totalChaptersCount = 24
        )

        // Disable Revision and Syllabus context
        val enabledCategories = setOf(
            BuddyContextCategory.PROFILE_GOALS,
            BuddyContextCategory.CALENDAR_PLANS
        )

        val envelope = BuddyContextEnvelope(
            queryIntent = BuddyQueryIntent.REVISION,
            verifiedFacts = facts,
            timeReference = "Sunday, 25 October 2026, 05:00 PM",
            enabledCategories = enabledCategories
        )

        val instruction = StudyBuddyContextEngine.buildSystemInstruction(envelope)

        // Profile & Goals is enabled
        assertTrue(instruction.contains("Tanvir"))
        assertTrue(instruction.contains("CATEGORY: Profile & Goals [ENABLED]"))

        // Revision Tasks was disabled: ensure it is marked disabled and prompt forbids hallucinating it
        assertTrue(instruction.contains("CATEGORY: Revision Tasks [DISABLED BY STUDENT]"))
        assertFalse(instruction.contains("5 items due"))

        // Syllabus & Chapters was disabled
        assertTrue(instruction.contains("CATEGORY: Syllabus & Chapters [DISABLED BY STUDENT]"))
        assertFalse(instruction.contains("12/24 chapters completed"))
    }

    @Test
    fun testGroundingScenarios() {
        // Scenario 1: Zero revision tasks
        val zeroRevFacts = StudyHubVerifiedFacts(dueRevisionsCount = 0)
        val env1 = BuddyContextEnvelope(
            queryIntent = BuddyQueryIntent.REVISION,
            verifiedFacts = zeroRevFacts,
            timeReference = "2026-10-25"
        )
        val instr1 = StudyBuddyContextEngine.buildSystemInstruction(env1)
        assertTrue(instr1.contains("0 items due"))

        // Scenario 2: Empty Calendar
        val emptyCalFacts = StudyHubVerifiedFacts(todayPlannedTasksCount = 0)
        val env2 = BuddyContextEnvelope(
            queryIntent = BuddyQueryIntent.PLANNING,
            verifiedFacts = emptyCalFacts,
            timeReference = "2026-10-25"
        )
        val instr2 = StudyBuddyContextEngine.buildSystemInstruction(env2)
        assertTrue(instr2.contains("(No tasks scheduled for today)"))

        // Scenario 3: Unavailable exam date
        val noExamFacts = StudyHubVerifiedFacts(nextExamTarget = null)
        val env3 = BuddyContextEnvelope(
            queryIntent = BuddyQueryIntent.EXAM_PREPARATION,
            verifiedFacts = noExamFacts,
            timeReference = "2026-10-25"
        )
        val instr3 = StudyBuddyContextEngine.buildSystemInstruction(env3)
        assertTrue(instr3.contains("No upcoming exam date recorded."))

        // Scenario 4: Incomplete history / 0 focus minutes
        val noHistoryFacts = StudyHubVerifiedFacts(todayStudyMinutesRecorded = 0, totalStudyHoursRecorded = 0f)
        val env4 = BuddyContextEnvelope(
            queryIntent = BuddyQueryIntent.FOCUS_HABITS,
            verifiedFacts = noHistoryFacts,
            timeReference = "2026-10-25"
        )
        val instr4 = StudyBuddyContextEngine.buildSystemInstruction(env4)
        assertTrue(instr4.contains("0 minutes"))
        assertTrue(instr4.contains("0.0 hours"))
    }

    @Test
    fun testSuggestedActions() {
        val facts = StudyHubVerifiedFacts(dueRevisionsCount = 2)

        val planningActions = StudyBuddyContextEngine.suggestActions(BuddyQueryIntent.PLANNING, facts)
        assertTrue(planningActions.any { it.actionType == BuddyActionType.OPEN_CALENDAR })

        val revisionActions = StudyBuddyContextEngine.suggestActions(BuddyQueryIntent.REVISION, facts)
        assertTrue(revisionActions.any { it.actionType == BuddyActionType.OPEN_REVISION_HUB })
        assertTrue(revisionActions.any { it.actionType == BuddyActionType.START_FOCUS })

        val examActions = StudyBuddyContextEngine.suggestActions(BuddyQueryIntent.EXAM_PREPARATION, facts)
        assertTrue(examActions.any { it.actionType == BuddyActionType.OPEN_ADMISSION_CALENDAR })
    }

    @Test
    fun testAutomaticContextPreparation() {
        val facts = StudyHubVerifiedFacts(
            studentName = "Nabil",
            academicLevel = "SSC 2026",
            admissionTrack = "General",
            dailyGoalHours = 3.0f,
            todayPlannedTasksCount = 1,
            todayPlannedTasksSummary = listOf("Math Chapter 1 Practice"),
            totalSubjectsCount = 4,
            totalChaptersCount = 20,
            completedChaptersCount = 8,
            incompleteChaptersCount = 12,
            dueRevisionsCount = 2,
            dueRevisionSubjects = listOf("Physics"),
            todayStudyMinutesRecorded = 45,
            totalStudyHoursRecorded = 12.0f,
            nextExamTarget = "Board Exam on 2026-12-01"
        )

        // Envelope defaults to all 8 categories enabled automatically
        val envelope = BuddyContextEnvelope(
            queryIntent = BuddyQueryIntent.GENERAL,
            verifiedFacts = facts,
            timeReference = "Monday, 26 October 2026, 09:00 AM"
        )

        val instruction = StudyBuddyContextEngine.buildSystemInstruction(envelope)

        assertEquals(8, envelope.enabledCategories.size)
        assertTrue(instruction.contains("CATEGORY: Profile & Goals [ENABLED]"))
        assertTrue(instruction.contains("CATEGORY: Syllabus & Chapters [ENABLED]"))
        assertTrue(instruction.contains("CATEGORY: Revision Tasks [ENABLED]"))
        assertTrue(instruction.contains("CATEGORY: Calendar & Plans [ENABLED]"))
        assertTrue(instruction.contains("CATEGORY: Focus & Study History [ENABLED]"))
        assertTrue(instruction.contains("CATEGORY: Stats & Progress [ENABLED]"))
        assertTrue(instruction.contains("CATEGORY: Exams & Admission [ENABLED]"))
        assertTrue(instruction.contains("CATEGORY: Notes & Materials [ENABLED]"))
    }
}
