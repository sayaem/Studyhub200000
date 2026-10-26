package com.example.buddy

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Context Engine that inspects verified local facts, classifies user intent,
 * formats grounded system instructions adhering to student context selection,
 * and suggests safe in-app actions.
 */
object StudyBuddyContextEngine {

    /**
     * Classifies user intent from prompt keywords.
     */
    fun classifyIntent(prompt: String): BuddyQueryIntent {
        val lower = prompt.lowercase(Locale.ROOT)
        return when {
            lower.contains("revis") || lower.contains("recall") || lower.contains("spaced repetition") || lower.contains("review") -> {
                BuddyQueryIntent.REVISION
            }
            lower.contains("plan") || lower.contains("schedule") || lower.contains("today") || lower.contains("routine") || lower.contains("missed") -> {
                BuddyQueryIntent.PLANNING
            }
            lower.contains("exam") || lower.contains("admission") || lower.contains("test") || lower.contains("deadline") || lower.contains("buet") || lower.contains("medical") || lower.contains("varsity") -> {
                BuddyQueryIntent.EXAM_PREPARATION
            }
            lower.contains("focus") || lower.contains("pomodoro") || lower.contains("distract") || lower.contains("concentrat") || lower.contains("timer") -> {
                BuddyQueryIntent.FOCUS_HABITS
            }
            lower.contains("stat") || lower.contains("progress") || lower.contains("hour") || lower.contains("time") || lower.contains("score") || lower.contains("analytics") -> {
                BuddyQueryIntent.STATS_PROGRESS
            }
            lower.contains("chapter") || lower.contains("topic") || lower.contains("syllabus") || lower.contains("subject") || lower.contains("physics") || lower.contains("chemistry") || lower.contains("math") || lower.contains("biology") -> {
                BuddyQueryIntent.TOPIC_SYLLABUS
            }
            else -> BuddyQueryIntent.GENERAL
        }
    }

    /**
     * Builds the system instruction prompt strictly grounded in verified facts,
     * respecting student-selected context categories and truthfulness boundaries.
     */
    fun buildSystemInstruction(envelope: BuddyContextEnvelope): String {
        val facts = envelope.verifiedFacts
        val enabled = envelope.enabledCategories
        val sb = StringBuilder()

        sb.appendLine("You are Study Buddy, a warm, pragmatic, encouraging, and deeply context-aware study companion inside the StudyHub Android application.")
        sb.appendLine("You assist Bangladeshi and international HSC, SSC, and university admission students.")
        sb.appendLine()
        sb.appendLine("=== LANGUAGE GUIDELINES ===")
        sb.appendLine("Match the student's language naturally:")
        sb.appendLine("- If the student writes in Bangla or Banglish, reply warmly and naturally in Bangla / Banglish.")
        sb.appendLine("- If the student writes in English, reply in clean, accessible English.")
        sb.appendLine()
        sb.appendLine("=== RESPONSE FORMATTING & ARTIFACT SCHEMA (MANDATORY) ===")
        sb.appendLine("You can return responses as structured JSON artifacts or standard clean Markdown.")
        sb.appendLine("For complex queries (study plans, statistics, quizzes, checklists, revision overviews, step-by-step math/science explanations), format your response as a JSON object inside ```json ... ``` with this structure:")
        sb.appendLine("{")
        sb.appendLine("  \"summary\": \"Direct warm summary answering the user...\",")
        sb.appendLine("  \"blocks\": [")
        sb.appendLine("    { \"type\": \"insight\", \"title\": \"Key Insight\", \"text\": \"...\", \"level\": \"INFO|SUCCESS|WARNING|HIGHLIGHT\" },")
        sb.appendLine("    { \"type\": \"stat_card\", \"title\": \"Today's Metrics\", \"metrics\": [ { \"label\": \"Today Study\", \"value\": \"1h 45m\", \"sublabel\": \"Goal: 2h\", \"icon\": \"⏱️\" } ] },")
        sb.appendLine("    { \"type\": \"step_by_step\", \"title\": \"Derivation of Resultant Vector\", \"steps\": [ { \"stepNumber\": 1, \"title\": \"Vector Components\", \"content\": \"Resolve A and B into rectangular components...\", \"isCrucial\": false }, { \"stepNumber\": 2, \"title\": \"Pythagorean Combination\", \"content\": \"Combine: R = \\\\sqrt{R_x^2 + R_y^2}\", \"isCrucial\": true } ], \"keyTakeaway\": \"Remember to check vector directions in quadrant II.\" },")
        sb.appendLine("    { \"type\": \"formula_card\", \"title\": \"Newton's Gravitational Law\", \"formulaLatex\": \"F = G \\\\frac{m_1 m_2}{r^2}\", \"variables\": [ { \"symbol\": \"G\", \"name\": \"Universal Gravitational Constant\", \"unit\": \"N m^2 / kg^2\" }, { \"symbol\": \"r\", \"name\": \"Distance between centers\", \"unit\": \"m\" } ], \"notes\": \"Inverse-square law applies to point masses.\", \"example\": \"Double the distance reduces force to 1/4.\" },")
        sb.appendLine("    { \"type\": \"flashcard\", \"front\": \"What is the principle of conservation of linear momentum?\", \"back\": \"In the absence of an external force, the total momentum of a closed system remains constant.\", \"tag\": \"Physics\", \"hint\": \"Newton's 3rd Law consequence\" },")
        sb.appendLine("    { \"type\": \"warning\", \"title\": \"Common Exam Pitfall\", \"cautionText\": \"Students frequently forget to convert km/h to m/s before applying kinematics formulas.\", \"commonMistake\": \"Using v = 72 km/h directly with t in seconds.\", \"correctApproach\": \"Multiply by (5/18) to get 20 m/s first.\" },")
        sb.appendLine("    { \"type\": \"concept_map\", \"topic\": \"Thermodynamic Processes\", \"nodes\": [ { \"name\": \"Isothermal\", \"description\": \"Constant Temperature (\\\\Delta T = 0, \\\\Delta U = 0)\", \"level\": 1 }, { \"name\": \"Adiabatic\", \"description\": \"No Heat Exchange (Q = 0)\", \"level\": 1 } ] },")
        sb.appendLine("    { \"type\": \"study_plan\", \"title\": \"Today's Study Schedule\", \"dateDescription\": \"TODAY\", \"steps\": [ { \"time\": \"04:00 PM\", \"title\": \"Physics Chapter 3 Revision\", \"subject\": \"Physics\", \"durationMinutes\": 30, \"isCompleted\": false } ] },")
        sb.appendLine("    { \"type\": \"checklist\", \"title\": \"Actionable Tasks\", \"items\": [ { \"id\": \"c1\", \"text\": \"Solve 5 problems\", \"isDone\": false } ] },")
        sb.appendLine("    { \"type\": \"quiz\", \"question\": \"Question text?\", \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"], \"correctIndex\": 0, \"explanation\": \"Explanation why A is correct...\" },")
        sb.appendLine("    { \"type\": \"expandable_detail\", \"title\": \"Step-by-Step Derivation\", \"content\": \"Detailed explanation in markdown...\" },")
        sb.appendLine("    { \"type\": \"action_group\", \"actions\": [ { \"actionType\": \"START_FOCUS|OPEN_REVISION_HUB|OPEN_CALENDAR|OPEN_SYLLABUS|OPEN_STATS\", \"title\": \"Start Focus Session\" } ] },")
        sb.appendLine("    { \"type\": \"follow_up\", \"suggestions\": [\"What should I revise next?\", \"Quiz me on Newton's Laws\"] }")
        sb.appendLine("  ]")
        sb.appendLine("}")
        sb.appendLine("For simple greetings (e.g. 'Hi', 'Hello'), reply with a short, warm, natural response without creating an unnecessary large artifact.")
        sb.appendLine()
        sb.appendLine("=== TRUTHFULNESS & GROUNDING RULES (MANDATORY) ===")
        sb.appendLine("1. Only use facts explicitly verified in the 'VERIFIED STUDENT CONTEXT' section below.")
        sb.appendLine("2. Never invent or hallucinate exams, grades, chapters, study hours, streaks, or deadlines.")
        sb.appendLine("3. Distinguish zero from unknown, unavailable, not initialized, and disabled:")
        sb.appendLine("   - If 0 revisions are due, state that no revision items are due today; do NOT assume the student has nothing to study.")
        sb.appendLine("   - If no tasks are scheduled, state that none are scheduled; offer calm assistance to schedule one.")
        sb.appendLine("   - If an exam date is unrecorded, state that no date is recorded yet; do NOT invent a test date.")
        sb.appendLine("   - If a category is marked [Disabled by student], you do NOT know that information and MUST NOT guess.")
        sb.appendLine("4. Never guilt or shame the student for missed sessions or zero hours; offer calm, actionable recovery steps.")
        sb.appendLine("5. Do not pretend you have read textbooks or private notes unless explicitly quoted.")
        sb.appendLine()
        sb.appendLine("=== CURRENT TIME REFERENCE ===")
        sb.appendLine("Current Date & Time: ${envelope.timeReference}")
        sb.appendLine()
        sb.appendLine("=== VERIFIED STUDENT CONTEXT (LOCAL APP DATA) ===")

        // 1. Profile & Goals
        if (enabled.contains(BuddyContextCategory.PROFILE_GOALS)) {
            sb.appendLine("--- CATEGORY: Profile & Goals [ENABLED] ---")
            sb.appendLine("- Student Name: ${facts.studentName ?: "Student"}")
            sb.appendLine("- Academic Level: ${facts.academicLevel ?: "Not specified"}")
            if (!facts.academicGroup.isNullOrBlank()) {
                sb.appendLine("- Academic Group: ${facts.academicGroup}")
            }
            sb.appendLine("- Target Admission Track: ${facts.admissionTrack ?: "General"}")
            sb.appendLine("- Daily Goal: ${facts.dailyGoalHours?.let { "$it hours/day" } ?: "Not set"}")
            sb.appendLine("- Preferred Study Time: ${facts.preferredStudyTime ?: "Flexible"}")
        } else {
            sb.appendLine("--- CATEGORY: Profile & Goals [DISABLED BY STUDENT] ---")
            sb.appendLine("[The student has disabled sharing Profile & Goals. Do not guess or claim to know their name, level, or goals.]")
        }

        // 2. Syllabus & Chapters
        if (enabled.contains(BuddyContextCategory.SYLLABUS_CHAPTERS)) {
            sb.appendLine("--- CATEGORY: Syllabus & Chapters [ENABLED] ---")
            sb.appendLine("- Syllabus Status: ${facts.completedChaptersCount}/${facts.totalChaptersCount} chapters completed across ${facts.totalSubjectsCount} subjects (${facts.incompleteChaptersCount} remaining).")
            if (facts.subjectSummaries.isNotEmpty()) {
                facts.subjectSummaries.forEach { sub ->
                    sb.appendLine("  • $sub")
                }
            }
            if (!facts.specificTopicName.isNullOrBlank()) {
                sb.appendLine("- Active Focus Topic: '${facts.specificTopicName}' in subject '${facts.specificSubjectName ?: "Syllabus"}'")
            }
        } else {
            sb.appendLine("--- CATEGORY: Syllabus & Chapters [DISABLED BY STUDENT] ---")
            sb.appendLine("[The student has disabled sharing Syllabus & Chapters. Do not invent subject names or chapter titles.]")
        }

        // 3. Smart Revision Tasks
        if (enabled.contains(BuddyContextCategory.REVISION_TASKS)) {
            sb.appendLine("--- CATEGORY: Revision Tasks [ENABLED] ---")
            sb.appendLine("- Due Revisions Today: ${facts.dueRevisionsCount} items due.")
            if (facts.dueRevisionSubjects.isNotEmpty()) {
                sb.appendLine("  Subjects needing revision: ${facts.dueRevisionSubjects.joinToString(", ")}")
            }
            if (facts.dueRevisionTopicsSummary.isNotEmpty()) {
                facts.dueRevisionTopicsSummary.forEach { topic ->
                    sb.appendLine("  • $topic")
                }
            }
            if (facts.retentionRatePercent != null) {
                sb.appendLine("- Retention Rate: ${facts.retentionRatePercent}% on-time completion.")
            }
        } else {
            sb.appendLine("--- CATEGORY: Revision Tasks [DISABLED BY STUDENT] ---")
            sb.appendLine("[The student has disabled sharing Revision Tasks. Do not guess due revision items.]")
        }

        // 4. Calendar & Plans
        if (enabled.contains(BuddyContextCategory.CALENDAR_PLANS)) {
            sb.appendLine("--- CATEGORY: Calendar & Plans [ENABLED] ---")
            sb.appendLine("- Today's Scheduled Tasks (${facts.todayPlannedTasksCount}):")
            if (facts.todayPlannedTasksSummary.isEmpty()) {
                sb.appendLine("  (No tasks scheduled for today)")
            } else {
                facts.todayPlannedTasksSummary.forEach { task ->
                    sb.appendLine("  • $task")
                }
            }
            if (facts.overdueTasksCount > 0) {
                sb.appendLine("- Overdue Tasks from Previous Days (${facts.overdueTasksCount}):")
                facts.overdueTasksSummary.forEach { task ->
                    sb.appendLine("  • $task")
                }
                sb.appendLine("  (Note: These tasks were scheduled on past dates and are overdue; do not claim they were scheduled for today.)")
            }
            if (facts.upcomingTasksSummary.isNotEmpty()) {
                sb.appendLine("- Upcoming Tasks:")
                facts.upcomingTasksSummary.forEach { task ->
                    sb.appendLine("  • $task")
                }
            }
        } else {
            sb.appendLine("--- CATEGORY: Calendar & Plans [DISABLED BY STUDENT] ---")
            sb.appendLine("[The student has disabled sharing Calendar & Plans. Do not invent scheduled tasks.]")
        }

        // 5. Focus & Study History
        if (enabled.contains(BuddyContextCategory.FOCUS_HISTORY)) {
            sb.appendLine("--- CATEGORY: Focus & Study History [ENABLED] ---")
            sb.appendLine("- Today's Recorded Study Time: ${facts.todayStudyMinutesRecorded} minutes.")
            if (facts.yesterdayStudyMinutesRecorded > 0) {
                sb.appendLine("- Yesterday's Recorded Study Time: ${facts.yesterdayStudyMinutesRecorded} minutes.")
            }
            sb.appendLine("- Total Study Time Logged: ${"%.1f".format(Locale.US, facts.totalStudyHoursRecorded)} hours.")
            sb.appendLine("- Focus Streak: ${facts.currentStreakDays} days (Longest: ${facts.longestStreakDays} days).")
            sb.appendLine("- Total Completed Sessions: ${facts.totalSessionsCompleted}.")
            if (facts.recentSessionsSummary.isNotEmpty()) {
                sb.appendLine("- Recent Sessions:")
                facts.recentSessionsSummary.forEach { sess ->
                    sb.appendLine("  • $sess")
                }
            }
        } else {
            sb.appendLine("--- CATEGORY: Focus & Study History [DISABLED BY STUDENT] ---")
            sb.appendLine("[The student has disabled sharing Focus & Study History. Do not invent study hours or streaks.]")
        }

        // 6. Stats & Progress
        if (enabled.contains(BuddyContextCategory.STATS_PROGRESS)) {
            sb.appendLine("--- CATEGORY: Stats & Progress [ENABLED] ---")
            val pct = if (facts.totalChaptersCount > 0) (facts.completedChaptersCount * 100 / facts.totalChaptersCount) else 0
            sb.appendLine("- Overall Syllabus Completion: $pct% (${facts.completedChaptersCount}/${facts.totalChaptersCount} chapters).")
            if (facts.retentionRatePercent != null) {
                sb.appendLine("- Spaced Repetition Retention: ${facts.retentionRatePercent}%.")
            }
        } else {
            sb.appendLine("--- CATEGORY: Stats & Progress [DISABLED BY STUDENT] ---")
            sb.appendLine("[The student has disabled sharing Stats & Progress.]")
        }

        // 7. Exams & Admission
        if (enabled.contains(BuddyContextCategory.EXAMS_ADMISSION)) {
            sb.appendLine("--- CATEGORY: Exams & Admission [ENABLED] ---")
            sb.appendLine("- Next Recorded Exam / Target: ${facts.nextExamTarget ?: "No upcoming exam date recorded."}")
            if (facts.allUpcomingExamsSummary.isNotEmpty()) {
                facts.allUpcomingExamsSummary.forEach { exam ->
                    sb.appendLine("  • $exam")
                }
            }
        } else {
            sb.appendLine("--- CATEGORY: Exams & Admission [DISABLED BY STUDENT] ---")
            sb.appendLine("[The student has disabled sharing Exams & Admission. Do not invent exam targets or dates.]")
        }

        // 8. Notes & Materials
        if (enabled.contains(BuddyContextCategory.NOTES_MATERIALS)) {
            sb.appendLine("--- CATEGORY: Notes & Materials [ENABLED] ---")
            sb.appendLine("- Saved Notes Count: ${facts.notesCount} notes recorded.")
            if (facts.recentNotesSummary.isNotEmpty()) {
                sb.appendLine("- Recent Note Topics:")
                facts.recentNotesSummary.forEach { note ->
                    sb.appendLine("  • $note")
                }
            }
        } else {
            sb.appendLine("--- CATEGORY: Notes & Materials [DISABLED BY STUDENT] ---")
            sb.appendLine("[The student has disabled sharing Notes & Materials.]")
        }

        if (!envelope.entryPointContext.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("- Handoff Context: ${envelope.entryPointContext}")
        }

        return sb.toString()
    }

    /**
     * Determines which contextual action chips should accompany the reply based on intent and facts.
     */
    fun suggestActions(intent: BuddyQueryIntent, facts: StudyHubVerifiedFacts): List<BuddyAction> {
        val actions = mutableListOf<BuddyAction>()

        when (intent) {
            BuddyQueryIntent.PLANNING -> {
                actions.add(BuddyAction(BuddyActionType.OPEN_CALENDAR, "Open Today's Calendar"))
                actions.add(BuddyAction(BuddyActionType.START_FOCUS, "Start a Focus Session"))
            }
            BuddyQueryIntent.REVISION -> {
                actions.add(BuddyAction(BuddyActionType.OPEN_REVISION_HUB, "Open Revision Hub"))
                if (facts.dueRevisionsCount > 0) {
                    actions.add(BuddyAction(BuddyActionType.START_FOCUS, "Start Revision Session"))
                }
            }
            BuddyQueryIntent.TOPIC_SYLLABUS -> {
                actions.add(BuddyAction(BuddyActionType.OPEN_SYLLABUS, "View Syllabus"))
                actions.add(BuddyAction(BuddyActionType.START_FOCUS, "Start Focus on Topic"))
            }
            BuddyQueryIntent.STATS_PROGRESS -> {
                actions.add(BuddyAction(BuddyActionType.OPEN_STATS, "View Detailed Analytics"))
            }
            BuddyQueryIntent.EXAM_PREPARATION -> {
                actions.add(BuddyAction(BuddyActionType.OPEN_ADMISSION_CALENDAR, "Check Exam Targets"))
                actions.add(BuddyAction(BuddyActionType.OPEN_CALENDAR, "Plan Exam Prep in Calendar"))
            }
            BuddyQueryIntent.FOCUS_HABITS -> {
                actions.add(BuddyAction(BuddyActionType.START_FOCUS, "Start Focus Timer"))
                actions.add(BuddyAction(BuddyActionType.OPEN_STATS, "View Focus Stats"))
            }
            BuddyQueryIntent.GENERAL -> {
                actions.add(BuddyAction(BuddyActionType.OPEN_CALENDAR, "Plan My Day"))
                actions.add(BuddyAction(BuddyActionType.START_FOCUS, "Start Focus Session"))
            }
        }

        return actions.distinctBy { it.actionType }
    }

    fun getCurrentTimeReference(): String {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }
}
