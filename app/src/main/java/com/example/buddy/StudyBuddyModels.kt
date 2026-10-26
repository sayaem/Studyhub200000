package com.example.buddy

/**
 * Supported intent classifications for Study Buddy context retrieval.
 */
enum class BuddyQueryIntent {
    TOPIC_SYLLABUS,
    PLANNING,
    STATS_PROGRESS,
    FOCUS_HABITS,
    REVISION,
    EXAM_PREPARATION,
    GENERAL
}

/**
 * Action type for safe in-app navigation and validated operations.
 */
enum class BuddyActionType {
    START_FOCUS,
    OPEN_CALENDAR,
    OPEN_SYLLABUS,
    OPEN_REVISION_HUB,
    OPEN_STATS,
    OPEN_ADMISSION_CALENDAR,
    OPEN_SUBJECT,
    ADD_TO_CALENDAR
}

/**
 * The 8 student-controlled context categories available in StudyHub.
 */
enum class BuddyContextCategory(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String
) {
    PROFILE_GOALS(
        id = "profile_goals",
        title = "Profile & Goals",
        description = "Name, academic level, target track & daily study goals",
        iconEmoji = "👤"
    ),
    SYLLABUS_CHAPTERS(
        id = "syllabus_chapters",
        title = "Syllabus & Chapters",
        description = "Registered subjects, chapters completed and remaining",
        iconEmoji = "📚"
    ),
    REVISION_TASKS(
        id = "revision_tasks",
        title = "Revision Tasks",
        description = "Spaced repetition workload, due topics & recall history",
        iconEmoji = "🧠"
    ),
    CALENDAR_PLANS(
        id = "calendar_plans",
        title = "Calendar & Plans",
        description = "Today's scheduled tasks, deadlines & study timetable",
        iconEmoji = "📅"
    ),
    FOCUS_HISTORY(
        id = "focus_history",
        title = "Focus & Study History",
        description = "Completed sessions, study minutes, streaks & focus logs",
        iconEmoji = "⏱️"
    ),
    STATS_PROGRESS(
        id = "stats_progress",
        title = "Stats & Progress",
        description = "Retention rate, daily study trends & chapter completion pace",
        iconEmoji = "📊"
    ),
    EXAMS_ADMISSION(
        id = "exams_admission",
        title = "Exams & Admission",
        description = "Target universities, confirmed exam dates & application deadlines",
        iconEmoji = "🎯"
    ),
    NOTES_MATERIALS(
        id = "notes_materials",
        title = "Notes & Materials",
        description = "Saved study notes, key formulas & topic summaries",
        iconEmoji = "📝"
    )
}

/**
 * Real-time availability info for a context category.
 */
data class ContextCategoryAvailability(
    val category: BuddyContextCategory,
    val isAvailable: Boolean,
    val summaryText: String
)

/**
 * An actionable chip proposed by Study Buddy.
 */
data class BuddyAction(
    val actionType: BuddyActionType,
    val title: String,
    val description: String = "",
    val targetSubjectId: Long? = null,
    val targetChapterId: Long? = null
)

/**
 * Verified StudyHub facts retrieved from local Room database.
 */
data class StudyHubVerifiedFacts(
    val studentName: String? = null,
    val academicLevel: String? = null,
    val academicGroup: String? = null,
    val targetYear: String? = null,
    val admissionTrack: String? = null,
    val dailyGoalHours: Float? = null,
    val preferredStudyTime: String? = null,
    val todayPlannedTasksCount: Int = 0,
    val todayPlannedTasksSummary: List<String> = emptyList(),
    val upcomingTasksSummary: List<String> = emptyList(),
    val totalSubjectsCount: Int = 0,
    val totalChaptersCount: Int = 0,
    val completedChaptersCount: Int = 0,
    val incompleteChaptersCount: Int = 0,
    val subjectSummaries: List<String> = emptyList(),
    val dueRevisionsCount: Int = 0,
    val dueRevisionSubjects: List<String> = emptyList(),
    val dueRevisionTopicsSummary: List<String> = emptyList(),
    val retentionRatePercent: Int? = null,
    val todayStudyMinutesRecorded: Int = 0,
    val totalStudyHoursRecorded: Float = 0f,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val totalSessionsCompleted: Int = 0,
    val recentSessionsSummary: List<String> = emptyList(),
    val nextExamTarget: String? = null,
    val allUpcomingExamsSummary: List<String> = emptyList(),
    val specificTopicName: String? = null,
    val specificSubjectName: String? = null,
    val notesCount: Int = 0,
    val recentNotesSummary: List<String> = emptyList(),
    val overdueTasksCount: Int = 0,
    val overdueTasksSummary: List<String> = emptyList(),
    val yesterdayStudyMinutesRecorded: Int = 0,
    val yesterdayGoalsMet: Boolean = false,
    val enabledCategories: Set<BuddyContextCategory> = BuddyContextCategory.entries.toSet(),
    val categoryAvailability: Map<BuddyContextCategory, ContextCategoryAvailability> = emptyMap()
)

/**
 * Contextual envelope attached to prompt requests.
 */
data class BuddyContextEnvelope(
    val queryIntent: BuddyQueryIntent,
    val verifiedFacts: StudyHubVerifiedFacts,
    val timeReference: String,
    val entryPointContext: String? = null,
    val enabledCategories: Set<BuddyContextCategory> = BuddyContextCategory.entries.toSet(),
    val allowedActions: List<BuddyActionType> = listOf(
        BuddyActionType.START_FOCUS,
        BuddyActionType.OPEN_CALENDAR,
        BuddyActionType.OPEN_SYLLABUS,
        BuddyActionType.OPEN_REVISION_HUB,
        BuddyActionType.OPEN_STATS,
        BuddyActionType.OPEN_ADMISSION_CALENDAR,
        BuddyActionType.OPEN_SUBJECT,
        BuddyActionType.ADD_TO_CALENDAR
    )
)

/**
 * Represents a user-selected device file (image, PDF, text, document) attached to a chat prompt.
 */
data class AttachedFileData(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val base64Content: String? = null,
    val textContent: String? = null
)

/**
 * Request payload sent to the AI Provider.
 */
data class BuddyGenerationRequest(
    val conversationId: Long,
    val userPrompt: String,
    val recentTurns: List<Pair<String, String>>, // sender (USER or ASSISTANT) to text
    val contextEnvelope: BuddyContextEnvelope,
    val attachedFiles: List<AttachedFileData> = emptyList()
)

/**
 * Result returned by the AI Provider.
 */
sealed interface BuddyAiResult {
    data class Success(
        val responseText: String,
        val suggestedActions: List<BuddyAction> = emptyList()
    ) : BuddyAiResult

    data class Error(
        val errorMessage: String,
        val isQuotaExhausted: Boolean = false,
        val isNetworkError: Boolean = false,
        val canRetry: Boolean = true
    ) : BuddyAiResult
}
