package com.example.buddy

/**
 * Data models for structured Study Buddy AI artifacts and response blocks.
 */

data class BuddyArtifactResponse(
    val summary: String? = null,
    val blocks: List<BuddyBlock> = emptyList()
)

sealed interface BuddyBlock {
    data class TextBlock(
        val content: String
    ) : BuddyBlock

    data class InsightBlock(
        val title: String,
        val text: String,
        val level: String = "INFO" // INFO, SUCCESS, WARNING, HIGHLIGHT
    ) : BuddyBlock

    data class StatCardBlock(
        val title: String? = null,
        val metrics: List<StatMetric>
    ) : BuddyBlock

    data class StudyPlanBlock(
        val title: String,
        val dateDescription: String? = null,
        val steps: List<StudyPlanStep>
    ) : BuddyBlock

    data class ChecklistBlock(
        val title: String,
        val items: List<ChecklistItem>
    ) : BuddyBlock

    data class QuizBlock(
        val question: String,
        val options: List<String>,
        val correctIndex: Int,
        val explanation: String
    ) : BuddyBlock

    data class StepByStepBlock(
        val title: String,
        val steps: List<StepItem>,
        val keyTakeaway: String? = null
    ) : BuddyBlock

    data class FormulaBlock(
        val title: String,
        val formulaLatex: String,
        val variables: List<FormulaVariable> = emptyList(),
        val notes: String? = null,
        val example: String? = null
    ) : BuddyBlock

    data class FlashcardBlock(
        val front: String,
        val back: String,
        val tag: String? = null,
        val hint: String? = null
    ) : BuddyBlock

    data class ConceptMapBlock(
        val topic: String,
        val nodes: List<ConceptNode>,
        val connections: List<String> = emptyList()
    ) : BuddyBlock

    data class WarningBlock(
        val title: String,
        val cautionText: String,
        val commonMistake: String? = null,
        val correctApproach: String? = null
    ) : BuddyBlock

    data class ExpandableBlock(
        val title: String,
        val content: String
    ) : BuddyBlock

    data class ActionGroupBlock(
        val actions: List<BuddyArtifactAction>
    ) : BuddyBlock

    data class FollowUpBlock(
        val suggestions: List<String>
    ) : BuddyBlock
}

data class StepItem(
    val stepNumber: Int,
    val title: String,
    val content: String,
    val isCrucial: Boolean = false
)

data class FormulaVariable(
    val symbol: String,
    val name: String,
    val unit: String? = null
)

data class ConceptNode(
    val name: String,
    val description: String,
    val level: Int = 0
)

data class StatMetric(
    val label: String,
    val value: String,
    val sublabel: String? = null,
    val icon: String? = null
)

data class StudyPlanStep(
    val time: String? = null,
    val title: String,
    val subject: String? = null,
    val durationMinutes: Int? = null,
    var isCompleted: Boolean = false
)

data class ChecklistItem(
    val id: String,
    val text: String,
    var isDone: Boolean = false
)

data class BuddyArtifactAction(
    val actionType: String,
    val title: String,
    val description: String? = null,
    val targetSubjectId: Long? = null,
    val targetChapterId: Long? = null,
    val scheduledDate: String? = null,
    val durationMins: Int? = null
)
