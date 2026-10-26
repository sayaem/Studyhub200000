package com.example

import com.example.data.local.StudyTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalendarEngineTest {

    private fun parseTimeToMinutes(timeStr: String?): Int = com.example.data.StudyHubDateUtils.parseTimeToMinutes(timeStr)

    private fun checkOverlap(start1: Int, dur1: Int, start2: Int, dur2: Int): Boolean {
        val end1 = start1 + dur1
        val end2 = start2 + dur2
        return maxOf(start1, start2) < minOf(end1, end2)
    }

    private fun formatMinutesToAmPm(totalMinutes: Int): String = com.example.data.StudyHubDateUtils.formatMinutesToAmPm(totalMinutes)

    private fun formatPlanTimeRange(timeStr: String?, durationMinutes: Int): String =
        com.example.data.StudyHubDateUtils.formatPlanTimeRange(timeStr, durationMinutes)

    @Test
    fun `time overlap detection correctly detects conflicting plans`() {
        // Plan 1: 08:00 AM to 09:30 AM (90 mins) -> 480 to 570
        val start1 = parseTimeToMinutes("08:00")
        val dur1 = 90

        // Plan 2: 09:00 AM to 10:00 AM (60 mins) -> 540 to 600 (Conflicts!)
        val start2 = parseTimeToMinutes("09:00")
        val dur2 = 60

        assertTrue(checkOverlap(start1, dur1, start2, dur2))

        // Plan 3: 09:30 AM to 11:00 AM (90 mins) -> 570 to 660 (Starts exactly when Plan 1 ends - No Conflict)
        val start3 = parseTimeToMinutes("09:30")
        val dur3 = 90

        assertFalse(checkOverlap(start1, dur1, start3, dur3))
    }

    @Test
    fun `format plan time range formats strictly on single continuous string`() {
        val formatted = formatPlanTimeRange("08:00", 60)
        assertEquals("08:00 AM - 09:00 AM • 1h 00m", formatted)

        val formatted2 = formatPlanTimeRange("14:30", 45)
        assertEquals("02:30 PM - 03:15 PM • 45m", formatted2)
    }

    @Test
    fun `auto suggest next available free time slot`() {
        val existingTasks = listOf(
            StudyTaskEntity(title = "Math", date = "2026-10-25", time = "08:00", durationMinutes = 60),
            StudyTaskEntity(title = "Physics", date = "2026-10-25", time = "09:00", durationMinutes = 45)
        )

        val latest = existingTasks.maxByOrNull { parseTimeToMinutes(it.time) + it.durationMinutes }
        assertNotNull(latest)
        val endMins = parseTimeToMinutes(latest!!.time) + latest.durationMinutes
        val nextStartH = endMins / 60
        val nextStartM = endMins % 60
        val nextFreeTimeStr = String.format("%02d:%02d", nextStartH, nextStartM)

        assertEquals("09:45", nextFreeTimeStr)
    }

    @Test
    fun `time parsing handles 24-hour strings accurately`() {
        assertEquals(8 * 60, parseTimeToMinutes("08:00"))
        assertEquals(13 * 60 + 30, parseTimeToMinutes("13:30"))
        assertEquals(0, parseTimeToMinutes("00:00"))
        assertEquals(23 * 60 + 59, parseTimeToMinutes("23:59"))
    }

    @Test
    fun `weekly seven-day sequence generates consecutive dates`() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, 2025)
        cal.set(Calendar.MONTH, Calendar.APRIL)
        cal.set(Calendar.DAY_OF_MONTH, 14) // Monday, 14 April 2025

        val days = (0..6).map { offset ->
            val dCal = cal.clone() as Calendar
            dCal.add(Calendar.DAY_OF_MONTH, offset)
            dCal.get(Calendar.DAY_OF_MONTH)
        }

        assertEquals(listOf(14, 15, 16, 17, 18, 19, 20), days)
    }

    @Test
    fun `time parsing handles 12-hour AM PM strings accurately`() {
        assertEquals(6 * 60, parseTimeToMinutes("06:00 AM"))
        assertEquals(7 * 60 + 45, parseTimeToMinutes("07:45 AM"))
        assertEquals(9 * 60 + 30, parseTimeToMinutes("09:30 AM"))
        assertEquals(10 * 60 + 45, parseTimeToMinutes("10:45 AM"))
        assertEquals(12 * 60, parseTimeToMinutes("12:00 PM"))
        assertEquals(13 * 60 + 30, parseTimeToMinutes("01:30 PM"))
        assertEquals(18 * 60, parseTimeToMinutes("06:00 PM"))
        assertEquals(0, parseTimeToMinutes("12:00 AM"))
    }

    @Test
    fun `study buddy generated schedule retains exact task start and end times - Acceptance Test`() {
        // Acceptance test from specification:
        // - Bangla: 06:00 AM, duration 90 minutes.
        // - English: 07:45 AM, duration 90 minutes.
        // - General Knowledge: 09:30 AM, duration 60 minutes.
        // - Admission MCQ Practice: 10:45 AM, duration 75 minutes.

        val task1 = StudyTaskEntity(
            title = "Bangla",
            date = "2026-10-26",
            time = com.example.data.StudyHubDateUtils.normalizeTimeForStorage("06:00 AM"),
            durationMinutes = 90
        )
        val task2 = StudyTaskEntity(
            title = "English",
            date = "2026-10-26",
            time = com.example.data.StudyHubDateUtils.normalizeTimeForStorage("07:45 AM"),
            durationMinutes = 90
        )
        val task3 = StudyTaskEntity(
            title = "General Knowledge",
            date = "2026-10-26",
            time = com.example.data.StudyHubDateUtils.normalizeTimeForStorage("09:30 AM"),
            durationMinutes = 60
        )
        val task4 = StudyTaskEntity(
            title = "Admission MCQ Practice",
            date = "2026-10-26",
            time = com.example.data.StudyHubDateUtils.normalizeTimeForStorage("10:45 AM"),
            durationMinutes = 75
        )

        // Verify start times are distinct and not default 08:00
        assertEquals("06:00", task1.time)
        assertEquals("07:45", task2.time)
        assertEquals("09:30", task3.time)
        assertEquals("10:45", task4.time)

        // Verify formatted time ranges for calendar UI display
        assertEquals("06:00 AM - 07:30 AM • 1h 30m", formatPlanTimeRange(task1.time, task1.durationMinutes))
        assertEquals("07:45 AM - 09:15 AM • 1h 30m", formatPlanTimeRange(task2.time, task2.durationMinutes))
        assertEquals("09:30 AM - 10:30 AM • 1h 00m", formatPlanTimeRange(task3.time, task3.durationMinutes))
        assertEquals("10:45 AM - 12:00 PM • 1h 15m", formatPlanTimeRange(task4.time, task4.durationMinutes))

        // Start time labels
        assertEquals("06:00 AM", com.example.data.StudyHubDateUtils.formatStartTimeOnly(task1.time))
        assertEquals("07:45 AM", com.example.data.StudyHubDateUtils.formatStartTimeOnly(task2.time))
        assertEquals("09:30 AM", com.example.data.StudyHubDateUtils.formatStartTimeOnly(task3.time))
        assertEquals("10:45 AM", com.example.data.StudyHubDateUtils.formatStartTimeOnly(task4.time))

        // Ensure NONE of them are at 8:00 AM
        val times = listOf(task1.time, task2.time, task3.time, task4.time)
        assertFalse("Calendar must not display all events at 08:00", times.all { it == "08:00" })
    }

    @Test
    fun `missing or invalid time safely falls back only for that specific task`() {
        val validTask = StudyTaskEntity(title = "Math", date = "2026-10-26", time = "07:00", durationMinutes = 60)
        val missingTimeTask = StudyTaskEntity(title = "General Study", date = "2026-10-26", time = null, durationMinutes = 45)

        assertEquals("07:00 AM - 08:00 AM • 1h 00m", formatPlanTimeRange(validTask.time, validTask.durationMinutes))
        // Missing time task uses documented fallback 8:00 AM only for itself
        assertEquals("08:00 AM - 08:45 AM • 45m", formatPlanTimeRange(missingTimeTask.time, missingTimeTask.durationMinutes))
    }

    @Test
    fun `study task entity defaults to STUDY plan type`() {
        val task = StudyTaskEntity(
            title = "Trigonometry",
            date = "2025-04-17",
            time = "08:00",
            durationMinutes = 90
        )

        assertEquals("STUDY", task.planType)
        assertEquals("PENDING", task.status)
        assertEquals(90, task.durationMinutes)
    }
}
