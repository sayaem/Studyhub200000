package com.example.ui.screens.buddy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buddy.BuddyAction
import com.example.buddy.BuddyActionType
import com.example.buddy.BuddyArtifactAction
import com.example.buddy.BuddyArtifactResponse
import com.example.buddy.BuddyBlock
import com.example.buddy.ChecklistItem
import com.example.buddy.StatMetric
import com.example.buddy.StudyPlanStep
import com.example.buddy.StepItem
import com.example.buddy.FormulaVariable
import com.example.buddy.ConceptNode

private val UserBubbleCyan = Color(0xFF0796A3)
private val DarkTextColor = Color(0xFF0F172A)
private val SubtitleColor = Color(0xFF64748B)
private val CardBorderColor = Color(0xFFE2E8F0)
private val AccentBorderCyan = Color(0xFF00D2C8)

@Composable
fun StudyBuddyArtifactRenderer(
    artifact: BuddyArtifactResponse,
    onExecuteAction: (BuddyAction) -> Unit = {},
    onSelectFollowUp: (String) -> Unit = {},
    onAddPlanToCalendar: (title: String, durationMins: Int, dateStr: String, timeStr: String?, subjectName: String?) -> Unit = { _, _, _, _, _ -> },
    onSaveToRevision: (subject: String, topic: String, question: String, explanation: String) -> Unit = { _, _, _, _ -> }
) {
    var showCalendarConfirmationForPlan by remember { mutableStateOf<BuddyBlock.StudyPlanBlock?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Optional summary intro text if present (and not duplicated in first block)
        if (!artifact.summary.isNullOrBlank() &&
            (artifact.blocks.isEmpty() || artifact.blocks.firstOrNull() !is BuddyBlock.TextBlock || (artifact.blocks.firstOrNull() as? BuddyBlock.TextBlock)?.content != artifact.summary)
        ) {
            StudyBuddyMarkdownRenderer(content = artifact.summary)
        }

        artifact.blocks.forEach { block ->
            when (block) {
                is BuddyBlock.TextBlock -> {
                    StudyBuddyMarkdownRenderer(content = block.content)
                }
                is BuddyBlock.InsightBlock -> {
                    InsightBlockView(block)
                }
                is BuddyBlock.StatCardBlock -> {
                    StatCardBlockView(block)
                }
                is BuddyBlock.StepByStepBlock -> {
                    StepByStepBlockView(block, onSelectFollowUp)
                }
                is BuddyBlock.FormulaBlock -> {
                    FormulaBlockView(block)
                }
                is BuddyBlock.FlashcardBlock -> {
                    FlashcardBlockView(block)
                }
                is BuddyBlock.ConceptMapBlock -> {
                    ConceptMapBlockView(block)
                }
                is BuddyBlock.WarningBlock -> {
                    WarningBlockView(block)
                }
                is BuddyBlock.StudyPlanBlock -> {
                    StudyPlanBlockView(
                        planBlock = block,
                        onAddCalendarClick = { showCalendarConfirmationForPlan = block },
                        onExecuteAction = onExecuteAction
                    )
                }
                is BuddyBlock.ChecklistBlock -> {
                    ChecklistBlockView(block)
                }
                is BuddyBlock.QuizBlock -> {
                    QuizBlockView(block, onSaveToRevision, onSelectFollowUp)
                }
                is BuddyBlock.ExpandableBlock -> {
                    ExpandableBlockView(block)
                }
                is BuddyBlock.ActionGroupBlock -> {
                    ActionGroupBlockView(block, onExecuteAction, onAddPlanToCalendar)
                }
                is BuddyBlock.FollowUpBlock -> {
                    FollowUpBlockView(block, onSelectFollowUp)
                }
            }
        }
    }

    // Confirmation dialog for Consequential Actions (e.g., adding study plan steps to Calendar)
    showCalendarConfirmationForPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { showCalendarConfirmationForPlan = null },
            title = {
                Text(
                    text = "Add Study Plan to Calendar?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor
                )
            },
            text = {
                Column {
                    Text(
                        text = "This will add ${plan.steps.size} study tasks into your StudyHub calendar:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkTextColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    plan.steps.forEach { step ->
                        Text(
                            text = "• ${step.title} (${step.durationMinutes ?: 25} mins)",
                            style = MaterialTheme.typography.bodySmall,
                            color = SubtitleColor
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        plan.steps.forEach { step ->
                            onAddPlanToCalendar(
                                step.title,
                                step.durationMinutes ?: 25,
                                plan.dateDescription ?: "TODAY",
                                step.time,
                                step.subject
                            )
                        }
                        showCalendarConfirmationForPlan = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UserBubbleCyan)
                ) {
                    Text("Confirm & Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarConfirmationForPlan = null }) {
                    Text("Cancel", color = SubtitleColor)
                }
            }
        )
    }
}

@Composable
private fun InsightBlockView(block: BuddyBlock.InsightBlock) {
    val (bgColor, borderColor, iconColor) = when (block.level) {
        "SUCCESS" -> Triple(Color(0xFFF0FDF4), Color(0xFF86EFAC), Color(0xFF16A34A))
        "WARNING" -> Triple(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFD97706))
        "HIGHLIGHT" -> Triple(Color(0xFFF0F9FF), Color(0xFF7DD3FC), Color(0xFF0284C7))
        else -> Triple(Color(0xFFF8FAFC), CardBorderColor, UserBubbleCyan)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = when (block.level) {
                    "SUCCESS" -> Icons.Default.CheckCircle
                    "WARNING" -> Icons.Default.Info
                    else -> Icons.Default.Lightbulb
                },
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = StudyBuddyMarkdownParser.parseInlineMarkdown(block.text),
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkTextColor.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StatCardBlockView(block: BuddyBlock.StatCardBlock) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (!block.title.isNullOrBlank()) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                block.metrics.forEach { metric ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!metric.icon.isNullOrBlank()) {
                            Text(text = metric.icon, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = metric.value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = UserBubbleCyan
                        )
                        Text(
                            text = metric.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = SubtitleColor
                        )
                        if (!metric.sublabel.isNullOrBlank()) {
                            Text(
                                text = metric.sublabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = SubtitleColor.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyPlanBlockView(
    planBlock: BuddyBlock.StudyPlanBlock,
    onAddCalendarClick: () -> Unit,
    onExecuteAction: (BuddyAction) -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = UserBubbleCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = planBlock.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextColor
                    )
                }

                if (!planBlock.dateDescription.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = planBlock.dateDescription,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            planBlock.steps.forEachIndexed { index, step ->
                var isChecked by remember { mutableStateOf(step.isCompleted) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = {
                            isChecked = it
                            step.isCompleted = it
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = UserBubbleCyan,
                            uncheckedColor = SubtitleColor
                        ),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = StudyBuddyMarkdownParser.parseInlineMarkdown(step.title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTextColor,
                            textDecoration = if (isChecked) TextDecoration.LineThrough else null
                        )
                        if (!step.time.isNullOrBlank() || !step.subject.isNullOrBlank() || step.durationMinutes != null) {
                            Text(
                                text = listOfNotNull(step.time, step.subject, step.durationMinutes?.let { "$it mins" }).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = SubtitleColor
                            )
                        }
                    }

                    // 1-tap start focus session on this step
                    IconButton(
                        onClick = {
                            onExecuteAction(
                                BuddyAction(
                                    actionType = BuddyActionType.START_FOCUS,
                                    title = "Focus: ${step.title}",
                                    description = "Focus on ${step.title}"
                                )
                            )
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Focus on ${step.title}",
                            tint = UserBubbleCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddCalendarClick,
                colors = ButtonDefaults.buttonColors(containerColor = UserBubbleCyan),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_plan_to_calendar_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Plan to Calendar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ChecklistBlockView(block: BuddyBlock.ChecklistBlock) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = block.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = DarkTextColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            block.items.forEach { item ->
                var isDone by remember { mutableStateOf(item.isDone) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isDone = !isDone
                            item.isDone = isDone
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDone,
                        onCheckedChange = {
                            isDone = it
                            item.isDone = it
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = UserBubbleCyan,
                            uncheckedColor = SubtitleColor
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = StudyBuddyMarkdownParser.parseInlineMarkdown(item.text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkTextColor,
                        textDecoration = if (isDone) TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizBlockView(
    quiz: BuddyBlock.QuizBlock,
    onSaveToRevision: (subject: String, topic: String, question: String, explanation: String) -> Unit = { _, _, _, _ -> },
    onSelectFollowUp: (String) -> Unit = {}
) {
    var selectedOption by remember { mutableIntStateOf(-1) }
    var isSavedToRevision by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorderColor),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = UserBubbleCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Interactive Knowledge Check",
                        style = MaterialTheme.typography.labelSmall,
                        color = UserBubbleCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bookmark / Save to Revision Hub
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSavedToRevision) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                    modifier = Modifier.clickable {
                        if (!isSavedToRevision) {
                            onSaveToRevision("General", "Active Recall Question", quiz.question, quiz.explanation)
                            isSavedToRevision = true
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSavedToRevision) Icons.Default.Check else Icons.Default.BookmarkBorder,
                            contentDescription = "Save to Revision Hub",
                            tint = if (isSavedToRevision) Color(0xFF16A34A) else SubtitleColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSavedToRevision) "Saved" else "Save to Revision",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSavedToRevision) Color(0xFF16A34A) else SubtitleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = StudyBuddyMarkdownParser.parseInlineMarkdown(quiz.question),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = DarkTextColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            quiz.options.forEachIndexed { index, optionText ->
                val isSelected = selectedOption == index
                val isCorrect = index == quiz.correctIndex
                val hasAnswered = selectedOption != -1

                val (btnBg, btnBorder, btnTextColor) = when {
                    hasAnswered && isCorrect -> Triple(Color(0xFFDCFCE7), Color(0xFF22C55E), Color(0xFF15803D))
                    hasAnswered && isSelected && !isCorrect -> Triple(Color(0xFFFEE2E2), Color(0xFFEF4444), Color(0xFFB91C1C))
                    isSelected -> Triple(Color(0xFFE0F2FE), AccentBorderCyan, UserBubbleCyan)
                    else -> Triple(Color(0xFFF8FAFC), CardBorderColor, DarkTextColor)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = btnBg,
                    border = BorderStroke(1.dp, btnBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = !hasAnswered) { selectedOption = index }
                        .testTag("quiz_option_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (hasAnswered && isCorrect) Color(0xFF22C55E) else Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ('A' + index).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (hasAnswered && isCorrect) Color.White else SubtitleColor
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = StudyBuddyMarkdownParser.parseInlineMarkdown(optionText),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = btnTextColor,
                            modifier = Modifier.weight(1f)
                        )

                        if (hasAnswered) {
                            if (isCorrect) {
                                Icon(Icons.Default.Check, contentDescription = "Correct", tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                            } else if (isSelected) {
                                Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = Color(0xFFB91C1C), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedOption != -1) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedOption == quiz.correctIndex) "🎉 Correct!" else "💡 Explanation:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedOption == quiz.correctIndex) Color(0xFF16A34A) else UserBubbleCyan
                                )
                                if (selectedOption != quiz.correctIndex) {
                                    TextButton(
                                        onClick = { selectedOption = -1 },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Try Again", color = UserBubbleCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = StudyBuddyMarkdownParser.parseInlineMarkdown(quiz.explanation),
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextColor,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableBlockView(block: BuddyBlock.ExpandableBlock) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand details",
                    tint = SubtitleColor
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    StudyBuddyMarkdownRenderer(content = block.content)
                }
            }
        }
    }
}

@Composable
private fun ActionGroupBlockView(
    block: BuddyBlock.ActionGroupBlock,
    onExecuteAction: (BuddyAction) -> Unit,
    onAddPlanToCalendar: (title: String, durationMins: Int, dateStr: String, timeStr: String?, subjectName: String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(block.actions) { act ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE0F2FE),
                border = BorderStroke(1.dp, AccentBorderCyan),
                modifier = Modifier
                    .clickable {
                        val buddyActionType = when (act.actionType.uppercase()) {
                            "START_FOCUS" -> BuddyActionType.START_FOCUS
                            "OPEN_CALENDAR" -> BuddyActionType.OPEN_CALENDAR
                            "OPEN_SYLLABUS" -> BuddyActionType.OPEN_SYLLABUS
                            "OPEN_REVISION_HUB" -> BuddyActionType.OPEN_REVISION_HUB
                            "OPEN_STATS" -> BuddyActionType.OPEN_STATS
                            "OPEN_ADMISSION_CALENDAR" -> BuddyActionType.OPEN_ADMISSION_CALENDAR
                            "OPEN_SUBJECT" -> BuddyActionType.OPEN_SUBJECT
                            "ADD_TO_CALENDAR" -> BuddyActionType.ADD_TO_CALENDAR
                            else -> BuddyActionType.OPEN_CALENDAR
                        }
                        onExecuteAction(
                            BuddyAction(
                                actionType = buddyActionType,
                                title = act.title,
                                description = act.description ?: "",
                                targetSubjectId = act.targetSubjectId,
                                targetChapterId = act.targetChapterId
                            )
                        )
                    }
                    .testTag("artifact_action_${act.actionType}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = UserBubbleCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = act.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1)
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowUpBlockView(
    block: BuddyBlock.FollowUpBlock,
    onSelectFollowUp: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Suggested Follow-ups:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SubtitleColor,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(block.suggestions) { suggestion ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .clickable { onSelectFollowUp(suggestion) }
                        .testTag("follow_up_chip_$suggestion")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = UserBubbleCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkTextColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepByStepBlockView(
    block: BuddyBlock.StepByStepBlock,
    onSelectFollowUp: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = UserBubbleCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextColor
                    )
                    Text(
                        text = "${block.steps.size} Structured Learning Steps",
                        style = MaterialTheme.typography.labelSmall,
                        color = SubtitleColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            block.steps.forEachIndexed { index, step ->
                var isExpanded by remember { mutableStateOf(index == 0 || step.isCrucial) }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (step.isCrucial) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (step.isCrucial) Color(0xFFFDE68A) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (step.isCrucial) Color(0xFFD97706) else UserBubbleCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${step.stepNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextColor
                                )
                            }

                            if (step.isCrucial) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = "Crucial",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SubtitleColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                StudyBuddyMarkdownRenderer(content = step.content)

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = {
                                            onSelectFollowUp("Explain step ${step.stepNumber}: ${step.title} in more detail")
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "🔍 Explain this step",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = UserBubbleCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!block.keyTakeaway.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = block.keyTakeaway,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormulaBlockView(block: BuddyBlock.FormulaBlock) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = null,
                        tint = UserBubbleCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main LaTeX Equation Card with copy action
            DisplayMathCard(mathLatex = block.formulaLatex)

            if (block.variables.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Variables & Units:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SubtitleColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.variables.forEach { v ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "•  ${v.symbol}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = UserBubbleCyan
                            )
                            Text(
                                text = " : ${v.name}${if (!v.unit.isNullOrBlank()) " (${v.unit})" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DarkTextColor
                            )
                        }
                    }
                }
            }

            if (!block.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📌 Note: ${block.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SubtitleColor
                )
            }

            if (!block.example.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Example: ${block.example}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FlashcardBlockView(block: BuddyBlock.FlashcardBlock) {
    var isFlipped by remember { mutableStateOf(false) }
    var masteryStatus by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (isFlipped) AccentBorderCyan else CardBorderColor),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = UserBubbleCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Active Recall Flashcard",
                        style = MaterialTheme.typography.labelSmall,
                        color = UserBubbleCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!block.tag.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = block.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card body
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isFlipped) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isFlipped) Color(0xFF86EFAC) else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isFlipped = !isFlipped }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (!isFlipped) "QUESTION / PROMPT" else "ANSWER / DEFINITION",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFlipped) Color(0xFF16A34A) else SubtitleColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (!isFlipped) block.front else block.back,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkTextColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    if (!isFlipped && !block.hint.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 Hint: ${block.hint}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SubtitleColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = UserBubbleCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (!isFlipped) "Tap to flip & reveal answer" else "Tap to flip back",
                            style = MaterialTheme.typography.labelSmall,
                            color = UserBubbleCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Self-assessment rating once flipped
            AnimatedVisibility(visible = isFlipped) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "How well did you know this?",
                        style = MaterialTheme.typography.labelSmall,
                        color = SubtitleColor,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (masteryStatus == "NEEDS_REVIEW") Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { masteryStatus = "NEEDS_REVIEW" }
                        ) {
                            Text(
                                text = "⚠️ Still Learning",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (masteryStatus == "NEEDS_REVIEW") Color(0xFFB91C1C) else DarkTextColor,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (masteryStatus == "MASTERED") Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { masteryStatus = "MASTERED" }
                        ) {
                            Text(
                                text = "🎯 Mastered!",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (masteryStatus == "MASTERED") Color(0xFF15803D) else DarkTextColor,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConceptMapBlockView(block: BuddyBlock.ConceptMapBlock) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = UserBubbleCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Concept Breakdown: ${block.topic}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.nodes.forEach { node ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(UserBubbleCyan)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = node.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextColor
                                )
                                Text(
                                    text = node.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SubtitleColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarningBlockView(block: BuddyBlock.WarningBlock) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.5.dp, Color(0xFFFDE68A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = block.cautionText,
                style = MaterialTheme.typography.bodySmall,
                color = DarkTextColor,
                lineHeight = 18.sp
            )

            if (!block.commonMistake.isNullOrBlank() || !block.correctApproach.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!block.commonMistake.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "❌ Common Trap: ${block.commonMistake}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (!block.correctApproach.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✅ Correct Method: ${block.correctApproach}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

