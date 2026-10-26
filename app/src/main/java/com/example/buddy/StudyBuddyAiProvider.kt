package com.example.buddy

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

import com.example.ai.gemini.GeminiConfig
import com.example.ai.gemini.StudyBuddyCentralClient

/**
 * Interface defining the AI Provider for Study Buddy.
 * Allows switching between Gemini REST, backend proxy, or fake test providers without UI changes.
 */
interface StudyBuddyAiProvider {
    suspend fun generateReply(request: BuddyGenerationRequest): BuddyAiResult
}

/**
 * Intelligent production-ready REST API client with multi-key rotation and failover.
 * Delegates to the centralized StudyBuddyCentralClient.
 */
class GeminiRestAiProvider(
    private val centralClient: StudyBuddyCentralClient = StudyBuddyCentralClient()
) : StudyBuddyAiProvider {

    override suspend fun generateReply(request: BuddyGenerationRequest): BuddyAiResult {
        return centralClient.generateReply(request)
    }
}

/**
 * Deterministic Fake Provider for tests and offline development.
 */
class FakeStudyBuddyAiProvider(
    var simulatedResult: BuddyAiResult? = null
) : StudyBuddyAiProvider {

    override suspend fun generateReply(request: BuddyGenerationRequest): BuddyAiResult {
        simulatedResult?.let { return it }

        val facts = request.contextEnvelope.verifiedFacts
        val intent = request.contextEnvelope.queryIntent

        val reply = when (intent) {
            BuddyQueryIntent.PLANNING -> {
                "Here is your study plan based on your StudyHub tasks: You have ${facts.todayPlannedTasksCount} tasks planned for today. Start with the most challenging task first, followed by a focused 25-minute Pomodoro session!"
            }
            BuddyQueryIntent.REVISION -> {
                if (facts.dueRevisionsCount > 0) {
                    "You have ${facts.dueRevisionsCount} revision items due today across: ${facts.dueRevisionSubjects.joinToString(", ")}. I recommend tackling them with active recall!"
                } else {
                    "Great job! You have 0 pending revisions due right now. Keep up the consistent pace!"
                }
            }
            BuddyQueryIntent.EXAM_PREPARATION -> {
                if (facts.nextExamTarget != null) {
                    "Your upcoming target is ${facts.nextExamTarget}. Let's make sure high-weightage topics are well practiced."
                } else {
                    "You do not have any upcoming exam date recorded yet in Admission/Calendar. Would you like to set one?"
                }
            }
            else -> {
                "Hello ${facts.studentName ?: "there"}! I'm your Study Buddy. You have completed ${facts.completedChaptersCount}/${facts.totalChaptersCount} chapters. How can I help you study today?"
            }
        }

        return BuddyAiResult.Success(
            responseText = reply,
            suggestedActions = StudyBuddyContextEngine.suggestActions(intent, facts)
        )
    }
}
