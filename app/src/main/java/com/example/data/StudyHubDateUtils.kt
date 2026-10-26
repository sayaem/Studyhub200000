package com.example.data

import com.example.data.local.StudySessionEntity
import com.example.data.local.StudyTaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Authoritative, deterministic date and midnight transition utilities for StudyHub.
 * Handles local calendar day boundaries, cross-midnight duration splitting,
 * overdue task classification, and streak preservation without destructive operations.
 */
object StudyHubDateUtils {

    val DATE_FORMAT: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Parses a time string into minutes from midnight (0..1439).
     * Supports:
     * - 12-hour AM/PM formats: "06:00 AM", "6:00 AM", "01:30 PM", "12:00 PM", "12:00 AM", "6 PM", "6:00pm", etc.
     * - 24-hour formats: "06:00", "14:30", "23:59", "00:00", "06:00:00"
     * - Time ranges: "06:00 AM - 07:30 AM", "06:00-07:30" (parses the start time)
     * - If timeStr is null/blank/invalid, returns [defaultMinutes] (defaults to 8:00 AM = 480).
     */
    fun parseTimeToMinutes(timeStr: String?, defaultMinutes: Int = 8 * 60): Int {
        if (timeStr.isNullOrBlank()) return defaultMinutes
        val clean = timeStr.trim()
        val startToken = clean.split(Regex("[-–—]|\\bto\\b", RegexOption.IGNORE_CASE)).firstOrNull()?.trim() ?: clean
        return parseSingleTimeToMinutes(startToken) ?: defaultMinutes
    }

    /**
     * Parses explicit end time from a time range string, or null if no range is present.
     * e.g., "06:00 AM - 07:30 AM" -> 450 (07:30 AM)
     */
    fun parseExplicitEndTimeToMinutes(timeStr: String?): Int? {
        if (timeStr.isNullOrBlank()) return null
        val parts = timeStr.trim().split(Regex("[-–—]|\\bto\\b", RegexOption.IGNORE_CASE))
        if (parts.size >= 2) {
            val endToken = parts[1].trim()
            return parseSingleTimeToMinutes(endToken)
        }
        return null
    }

    /**
     * Parses a single time token into minutes from midnight (0..1439).
     * Returns null if unparseable.
     */
    fun parseSingleTimeToMinutes(token: String): Int? {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return null

        val regex = Regex("""^(\d{1,2})(?::(\d{1,2}))?(?::\d{1,2})?\s*(AM|PM)?$""", RegexOption.IGNORE_CASE)
        val match = regex.find(trimmed) ?: return null

        val hRaw = match.groupValues[1].toIntOrNull() ?: return null
        val mRaw = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
        val amPm = match.groupValues[3].uppercase(Locale.ROOT)

        var h = hRaw
        val m = mRaw.coerceIn(0, 59)

        if (amPm == "PM") {
            if (h < 12) h += 12
        } else if (amPm == "AM") {
            if (h == 12) h = 0
        }

        val totalMinutes = (h * 60) + m
        return ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    }

    /**
     * Formats total minutes of the day into 24-hour HH:mm string.
     */
    fun formatMinutesTo24H(totalMinutes: Int): String {
        val norm = (totalMinutes % (24 * 60) + (24 * 60)) % (24 * 60)
        val h = norm / 60
        val m = norm % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    /**
     * Formats total minutes into 12-hour AM/PM string, e.g. "06:00 AM", "12:00 PM".
     */
    fun formatMinutesToAmPm(totalMinutes: Int): String {
        val norm = (totalMinutes % (24 * 60) + (24 * 60)) % (24 * 60)
        val hour24 = norm / 60
        val minute = norm % 60
        val isPm = hour24 >= 12
        val hour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
        val amPm = if (isPm) "PM" else "AM"
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm)
    }

    /**
     * Formats start time only from a stored time string.
     */
    fun formatStartTimeOnly(timeStr: String?): String {
        val mins = parseTimeToMinutes(timeStr)
        return formatMinutesToAmPm(mins)
    }

    /**
     * Formats a plan's time range: start - end • duration
     * Preserves explicit end time if timeStr contains a range, otherwise calculates end time from durationMinutes.
     */
    fun formatPlanTimeRange(timeStr: String?, durationMinutes: Int): String {
        val startMins = parseTimeToMinutes(timeStr)
        val explicitEndMins = parseExplicitEndTimeToMinutes(timeStr)
        val effectiveDur = if (explicitEndMins != null) {
            val diff = explicitEndMins - startMins
            if (diff > 0) diff else (diff + 24 * 60) % (24 * 60)
        } else {
            durationMinutes
        }
        val endMins = explicitEndMins ?: ((startMins + effectiveDur) % (24 * 60))

        val startStr = formatMinutesToAmPm(startMins)
        val endStr = formatMinutesToAmPm(endMins)

        val durHours = effectiveDur / 60
        val durMins = effectiveDur % 60
        val durStr = when {
            durHours > 0 && durMins > 0 -> "${durHours}h ${durMins}m"
            durHours > 0 -> "${durHours}h 00m"
            else -> "${durMins}m"
        }

        return "$startStr - $endStr • $durStr"
    }

    /**
     * Normalizes an arbitrary time string (e.g., "06:00 AM", "6:00 AM", "14:30")
     * into a standard HH:mm 24-hour string for persistent storage, or null if unparseable.
     */
    fun normalizeTimeForStorage(timeStr: String?): String? {
        if (timeStr.isNullOrBlank()) return null
        val startToken = timeStr.trim().split(Regex("[-–—]|\\bto\\b", RegexOption.IGNORE_CASE)).first().trim()
        val mins = parseSingleTimeToMinutes(startToken) ?: return null
        return formatMinutesTo24H(mins)
    }

    /**
     * Gets the current date string (YYYY-MM-dd) based on reference time.
     */
    fun getTodayDateString(referenceMs: Long = System.currentTimeMillis()): String {
        return DATE_FORMAT.format(Date(referenceMs))
    }

    /**
     * Computes the YYYY-MM-dd date string of the previous day relative to referenceDateStr.
     */
    fun getYesterdayDateString(todayDateStr: String): String {
        return try {
            val date = DATE_FORMAT.parse(todayDateStr) ?: return ""
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.DAY_OF_YEAR, -1)
            }
            DATE_FORMAT.format(cal.time)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Returns the exact start (00:00:00.000) and end (23:59:59.999) timestamps for a given date string.
     */
    fun getDayStartAndEndMillis(dateStr: String): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            val parsed = try { DATE_FORMAT.parse(dateStr) } catch (e: Exception) { null }
            if (parsed != null) time = parsed
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis

        return Pair(startMs, endMs)
    }

    /**
     * Accurately calculates study duration attributable to a given calendar day window,
     * correctly splitting cross-midnight sessions at the midnight boundary.
     * Prevents double-counting and ensures daily attribution is mathematically sound.
     */
    fun calculateDayStudySeconds(
        sessions: List<StudySessionEntity>,
        dayStartMs: Long,
        dayEndMs: Long
    ): Int {
        val boundaryEndMs = if (dayEndMs % 1000L == 999L) dayEndMs + 1L else dayEndMs
        return sessions.filter { it.status == "COMPLETED" }.sumOf { session ->
            val start = session.startedAt
            val totalSec = session.elapsedSeconds
            if (totalSec <= 0) return@sumOf 0

            val rawSpanMs = maxOf(0L, session.endedAt - start)
            val effectiveDurationMs = if (rawSpanMs >= totalSec * 1000L) rawSpanMs else totalSec * 1000L
            val end = start + effectiveDurationMs

            // If session entirely falls on this calendar day:
            if (start >= dayStartMs && end <= boundaryEndMs) {
                totalSec
            } else {
                val overlapStart = maxOf(start, dayStartMs)
                val overlapEnd = minOf(end, boundaryEndMs)
                if (overlapEnd > overlapStart) {
                    val overlapMs = overlapEnd - overlapStart
                    val proportion = overlapMs.toDouble() / effectiveDurationMs.toDouble()
                    val overlapSec = kotlin.math.round(totalSec * proportion).toInt()
                    overlapSec.coerceIn(0, totalSec)
                } else {
                    0
                }
            }
        }
    }

    /**
     * Determines whether an incomplete task is overdue relative to the current local day.
     * Retains original due dates and lifecycle semantics.
     */
    fun isTaskOverdue(task: StudyTaskEntity, todayDateStr: String): Boolean {
        if (task.status == "COMPLETED" || task.status == "CANCELLED") return false
        return task.date < todayDateStr
    }

    /**
     * Filters overdue tasks from previous dates that remain incomplete.
     */
    fun filterOverdueTasks(tasks: List<StudyTaskEntity>, todayDateStr: String): List<StudyTaskEntity> {
        return tasks.filter { isTaskOverdue(it, todayDateStr) }
            .sortedWith(compareBy({ it.date }, { it.time ?: "99:99" }, { it.id }))
    }

    /**
     * Filters tasks originally scheduled for the specified calendar date.
     */
    fun filterTasksForDate(tasks: List<StudyTaskEntity>, dateStr: String): List<StudyTaskEntity> {
        return tasks.filter { it.date == dateStr }
            .sortedWith(compareBy({ it.time ?: "99:99" }, { it.id }))
    }

    /**
     * Calculates active streak days consistently:
     * - If last focus date is today -> active streak is currentStreakDays
     * - If last focus date is yesterday -> active streak is preserved at currentStreakDays
     * - If last focus date is older than yesterday -> streak is broken (0 active days)
     * - If last focus date is empty -> returns currentStreakDays for backward compatibility
     */
    fun calculateActiveStreak(
        lastFocusDateStr: String?,
        currentStreakDays: Int,
        todayDateStr: String
    ): Int {
        if (currentStreakDays <= 0) return 0
        if (lastFocusDateStr.isNullOrBlank()) return currentStreakDays
        if (lastFocusDateStr == todayDateStr) return currentStreakDays
        val yesterdayStr = getYesterdayDateString(todayDateStr)
        if (lastFocusDateStr == yesterdayStr) return currentStreakDays
        return 0
    }
}
