package com.example

import com.example.buddy.BuddyContextCategory
import com.example.buddy.BuddyContextEnvelope
import com.example.buddy.BuddyQueryIntent
import com.example.buddy.StudyBuddyContextEngine
import com.example.buddy.StudyHubVerifiedFacts
import com.example.data.StudyHubDateUtils
import com.example.data.local.StudySessionEntity
import com.example.data.local.StudyTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MidnightIntelligenceTest {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    @Test
    fun `cross-midnight session duration is accurately split at midnight boundary`() {
        val todayStr = "2026-10-26"
        val yesterdayStr = StudyHubDateUtils.getYesterdayDateString(todayStr)
        assertEquals("2026-10-25", yesterdayStr)

        val (yestStart, yestEnd) = StudyHubDateUtils.getDayStartAndEndMillis(yesterdayStr)
        val (todayStart, todayEnd) = StudyHubDateUtils.getDayStartAndEndMillis(todayStr)

        // Session starts at 23:45 on yesterday and ends at 00:15 on today (30 min total = 1800 sec)
        val sessionStart = yestStart + (23 * 3600 + 45 * 60) * 1000L // 23:45:00.000
        val sessionEnd = todayStart + (15 * 60) * 1000L // 00:15:00.000
        val elapsedSeconds = 1800

        val session = StudySessionEntity(
            id = 1L,
            subjectId = 101L,
            startedAt = sessionStart,
            endedAt = sessionEnd,
            elapsedSeconds = elapsedSeconds,
            status = "COMPLETED",
            sessionType = "POMODORO_FOCUS"
        )
        val sessionList = listOf(session)

        val yestSeconds = StudyHubDateUtils.calculateDayStudySeconds(sessionList, yestStart, yestEnd)
        val todaySeconds = StudyHubDateUtils.calculateDayStudySeconds(sessionList, todayStart, todayEnd)

        // Exactly 15 minutes (900 seconds) attributed to yesterday and 15 minutes (900 seconds) to today
        assertEquals(900, yestSeconds)
        assertEquals(900, todaySeconds)
        assertEquals(1800, yestSeconds + todaySeconds)
    }

    @Test
    fun `session entirely within yesterday is not counted towards today`() {
        val todayStr = "2026-10-26"
        val yesterdayStr = StudyHubDateUtils.getYesterdayDateString(todayStr)
        val (todayStart, todayEnd) = StudyHubDateUtils.getDayStartAndEndMillis(todayStr)
        val (yestStart, yestEnd) = StudyHubDateUtils.getDayStartAndEndMillis(yesterdayStr)

        val session = StudySessionEntity(
            id = 2L,
            subjectId = 101L,
            startedAt = yestStart + 3600_000L,
            endedAt = yestStart + 7200_000L,
            elapsedSeconds = 3600,
            status = "COMPLETED",
            sessionType = "DEEP_FOCUS"
        )
        val sessionList = listOf(session)

        val todaySeconds = StudyHubDateUtils.calculateDayStudySeconds(sessionList, todayStart, todayEnd)
        val yestSeconds = StudyHubDateUtils.calculateDayStudySeconds(sessionList, yestStart, yestEnd)

        assertEquals(0, todaySeconds)
        assertEquals(3600, yestSeconds)
    }

    @Test
    fun `streak is preserved from yesterday and only reset if yesterday was missed`() {
        val todayStr = "2026-10-26"
        val yesterdayStr = "2026-10-25"
        val twoDaysAgoStr = "2026-10-24"

        // Case 1: Studied today -> Active streak is currentStreakDays
        val streakToday = StudyHubDateUtils.calculateActiveStreak(
            lastFocusDateStr = todayStr,
            currentStreakDays = 5,
            todayDateStr = todayStr
        )
        assertEquals(5, streakToday)

        // Case 2: Studied yesterday, haven't studied yet today -> Streak is alive (preserved at 5, not 0, not incremented)
        val streakYesterday = StudyHubDateUtils.calculateActiveStreak(
            lastFocusDateStr = yesterdayStr,
            currentStreakDays = 5,
            todayDateStr = todayStr
        )
        assertEquals(5, streakYesterday)

        // Case 3: Last studied 2 days ago -> Streak is broken (0 active days)
        val streakBroken = StudyHubDateUtils.calculateActiveStreak(
            lastFocusDateStr = twoDaysAgoStr,
            currentStreakDays = 5,
            todayDateStr = todayStr
        )
        assertEquals(0, streakBroken)

        // Case 4: No prior sessions logged -> Streak is 0
        val streakEmpty = StudyHubDateUtils.calculateActiveStreak(
            lastFocusDateStr = null,
            currentStreakDays = 0,
            todayDateStr = todayStr
        )
        assertEquals(0, streakEmpty)
    }

    @Test
    fun `overdue tasks retain original dates and are distinguished from today tasks`() {
        val todayStr = "2026-10-26"
        val yesterdayStr = "2026-10-25"

        val task1 = StudyTaskEntity(
            id = 10L,
            subjectId = 1L,
            title = "Physics Mechanics Review",
            date = yesterdayStr,
            time = "14:00",
            durationMinutes = 45,
            priority = "HIGH",
            status = "PENDING"
        )
        val task2 = StudyTaskEntity(
            id = 11L,
            subjectId = 1L,
            title = "Chemistry Periodic Table",
            date = todayStr,
            time = "10:00",
            durationMinutes = 30,
            priority = "MEDIUM",
            status = "PENDING"
        )
        val task3 = StudyTaskEntity(
            id = 12L,
            subjectId = 2L,
            title = "Math Calculus Completed",
            date = yesterdayStr,
            time = "16:00",
            durationMinutes = 60,
            priority = "NORMAL",
            status = "COMPLETED"
        )

        val allTasks = listOf(task1, task2, task3)

        val overdueTasks = StudyHubDateUtils.filterOverdueTasks(allTasks, todayStr)
        val todayTasks = StudyHubDateUtils.filterTasksForDate(allTasks, todayStr)

        assertEquals(1, overdueTasks.size)
        assertEquals(10L, overdueTasks[0].id)
        assertEquals(yesterdayStr, overdueTasks[0].date) // Original date retained verbatim

        assertEquals(1, todayTasks.size)
        assertEquals(11L, todayTasks[0].id)
        assertEquals(todayStr, todayTasks[0].date)

        assertTrue(StudyHubDateUtils.isTaskOverdue(task1, todayStr))
        assertFalse(StudyHubDateUtils.isTaskOverdue(task2, todayStr))
        assertFalse(StudyHubDateUtils.isTaskOverdue(task3, todayStr)) // Completed tasks are never overdue
    }

    @Test
    fun `study buddy context engine grounds overdue tasks and yesterday study metrics`() {
        val facts = StudyHubVerifiedFacts(
            studentName = "Alex",
            todayPlannedTasksCount = 2,
            todayPlannedTasksSummary = listOf("Calculus Differentiation (PENDING)"),
            overdueTasksCount = 1,
            overdueTasksSummary = listOf("Physics Mechanics (due 2026-10-25, HIGH priority)"),
            todayStudyMinutesRecorded = 30,
            yesterdayStudyMinutesRecorded = 75,
            yesterdayGoalsMet = true,
            currentStreakDays = 4,
            enabledCategories = setOf(BuddyContextCategory.CALENDAR_PLANS, BuddyContextCategory.FOCUS_HISTORY)
        )

        val prompt = StudyBuddyContextEngine.buildSystemInstruction(
            envelope = BuddyContextEnvelope(
                queryIntent = BuddyQueryIntent.PLANNING,
                verifiedFacts = facts,
                timeReference = "Morning",
                entryPointContext = null,
                enabledCategories = facts.enabledCategories
            )
        )

        assertTrue(prompt.contains("Overdue Tasks from Previous Days (1)"))
        assertTrue(prompt.contains("Physics Mechanics (due 2026-10-25, HIGH priority)"))
        assertTrue(prompt.contains("Yesterday's Recorded Study Time: 75 minutes"))
        assertTrue(prompt.contains("Today's Recorded Study Time: 30 minutes"))
        assertTrue(prompt.contains("Focus Streak: 4 days"))
        assertTrue(prompt.contains("do not claim they were scheduled for today"))
    }
}
