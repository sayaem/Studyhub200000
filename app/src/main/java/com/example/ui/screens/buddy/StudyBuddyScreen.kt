package com.example.ui.screens.buddy

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.buddy.AttachedFileData
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buddy.BuddyAction
import com.example.buddy.BuddyBlock
import com.example.buddy.BuddyContextCategory
import com.example.buddy.ContextCategoryAvailability
import com.example.buddy.StudyBuddyArtifactParser
import com.example.data.local.BuddyConversationEntity
import com.example.data.local.BuddyMessageEntity
import com.example.data.local.UserProfile
import com.example.revision.RevisionSummaryMetrics
import com.example.ui.components.EmblemSize
import com.example.ui.components.StudyBuddyEmblem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom design colors matching StudyHub visual identity
private val SoftBackground = Color(0xFFF4F8FA)
private val UserBubbleCyan = Color(0xFF0796A3)
private val AssistantCardBg = Color(0xFFFFFFFF)
private val AssistantCardBorder = Color(0xFFE2E8F0)
private val AccentCyanBorder = Color(0xFF00D2C8)
private val DarkTextColor = Color(0xFF0F172A)
private val SubtitleColor = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudyBuddyScreen(
    userProfile: UserProfile?,
    conversations: List<BuddyConversationEntity>,
    currentConversationId: Long?,
    messages: List<BuddyMessageEntity>,
    isSending: Boolean,
    errorMessage: String?,
    suggestedActions: List<BuddyAction>,
    revisionMetrics: RevisionSummaryMetrics?,
    activeTopicContext: Pair<String?, String?>?,
    initialQueryText: String? = null,
    aiActivityText: String? = null,
    enabledContextCategories: Set<BuddyContextCategory> = BuddyContextCategory.entries.toSet(),
    contextAvailability: Map<BuddyContextCategory, ContextCategoryAvailability> = emptyMap(),
    onToggleContextCategory: (BuddyContextCategory) -> Unit = {},
    onSetAllContextCategories: (Boolean) -> Unit = {},
    onSendMessage: (String) -> Unit,
    onSendMessageWithContext: ((String, String?) -> Unit)? = null,
    onSendMessageWithAttachments: ((String, String?, List<AttachedFileData>) -> Unit)? = null,
    onStopGeneration: () -> Unit = {},
    onRetryMessage: () -> Unit,
    onClearError: () -> Unit,
    onStartNewChat: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onExecuteAction: (BuddyAction) -> Unit,
    onAddPlanToCalendar: (String, Int, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onSaveToRevision: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf(initialQueryText ?: "") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var attachedContextSnippet by remember { mutableStateOf<String?>(null) }
    var attachedContextLabel by remember { mutableStateOf<String?>(null) }
    var attachedFiles by remember { mutableStateOf<List<AttachedFileData>>(emptyList()) }
    var conversationToDelete by remember { mutableStateOf<BuddyConversationEntity?>(null) }

    // System document/photo picker launcher (OpenMultipleDocuments)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val loaded = mutableListOf<AttachedFileData>()
            for (uri in uris) {
                try {
                    var fileName = "attachment_${System.currentTimeMillis()}"
                    var fileSize = 0L
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                    // 10 MB per-file limit check
                    if (fileSize > 10 * 1024 * 1024) {
                        Toast.makeText(context, "$fileName exceeds 10 MB limit", Toast.LENGTH_SHORT).show()
                        continue
                    }

                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null && bytes.isNotEmpty()) {
                        val isImageOrPdf = mimeType.startsWith("image/") || mimeType == "application/pdf"
                        val base64 = if (isImageOrPdf) Base64.encodeToString(bytes, Base64.NO_WRAP) else null
                        val text = if (!isImageOrPdf) {
                            try { String(bytes, Charsets.UTF_8) } catch (_: Exception) { null }
                        } else null

                        if (attachedFiles.none { it.fileName == fileName } && loaded.none { it.fileName == fileName }) {
                            loaded.add(
                                AttachedFileData(
                                    fileName = fileName,
                                    mimeType = mimeType,
                                    sizeBytes = if (fileSize > 0) fileSize else bytes.size.toLong(),
                                    base64Content = base64,
                                    textContent = text
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            if (loaded.isNotEmpty()) {
                attachedFiles = (attachedFiles + loaded).take(5)
            }
        }
    }

    // Speech recognition launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spokenText else "$inputText $spokenText"
            }
        }
    }

    fun launchVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Study Buddy...")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voice input is not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    // Auto-scroll to bottom on new messages or keyboard open
    LaunchedEffect(messages.size, isSending, imeBottom) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Auto-send initial prompt if provided
    LaunchedEffect(initialQueryText) {
        if (!initialQueryText.isNullOrBlank() && messages.isEmpty()) {
            if (onSendMessageWithAttachments != null) {
                onSendMessageWithAttachments(initialQueryText, null, emptyList())
            } else if (onSendMessageWithContext != null) {
                onSendMessageWithContext(initialQueryText, null)
            } else {
                onSendMessage(initialQueryText)
            }
            inputText = ""
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StudyBuddyEmblem(size = EmblemSize.TOOLBAR)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Study Buddy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextColor
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Online. Grounded AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SubtitleColor
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("buddy_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkTextColor
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAttachmentDialog = true },
                        modifier = Modifier.testTag("buddy_study_context_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Study Context",
                            tint = DarkTextColor
                        )
                    }
                    IconButton(
                        onClick = onStartNewChat,
                        modifier = Modifier.testTag("buddy_new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Conversation",
                            tint = UserBubbleCyan
                        )
                    }
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("buddy_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Conversation History",
                            tint = DarkTextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftBackground)
                .padding(top = innerPadding.calculateTopPadding())
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
        ) {
            // Context pill if user came from a specific topic/subject
            if (activeTopicContext?.first != null) {
                Surface(
                    color = Color(0xFFE0F2FE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📌 Context Focus: ${activeTopicContext.first} (${activeTopicContext.second ?: "Syllabus"})",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF0369A1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Central scrollable area (Messages or Welcome Screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (messages.isEmpty()) {
                    // Empty Welcome State (Screen A)
                    EmptyBuddyWelcomeState(
                        studentName = userProfile?.displayName,
                        revisionMetrics = revisionMetrics,
                        enabledContextCount = enabledContextCategories.size,
                        onOpenContextSheet = { showAttachmentDialog = true },
                        onSelectPrompt = { prompt ->
                            if (onSendMessageWithAttachments != null) {
                                onSendMessageWithAttachments(prompt, null, emptyList())
                            } else if (onSendMessageWithContext != null) {
                                onSendMessageWithContext(prompt, null)
                            } else {
                                onSendMessage(prompt)
                            }
                        }
                    )
                } else {
                    // Active Conversation List (Screen B)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            BuddyMessageBubble(
                                message = msg,
                                onExecuteAction = onExecuteAction,
                                onSendMessage = { prompt -> onSendMessage(prompt) },
                                onAddPlanToCalendar = onAddPlanToCalendar,
                                onSaveToRevision = onSaveToRevision,
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Study Buddy", msg.content)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onRetry = onRetryMessage
                            )
                        }

                        if (isSending) {
                            item {
                                BuddyThinkingIndicator(
                                    activityText = aiActivityText ?: "Preparing your response"
                                )
                            }
                        }
                    }
                }
            }

            // Error banner if any
            AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B)
                            )
                        }
                        IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = Color(0xFF991B1B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Action chips from the latest assistant response
            if (suggestedActions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(suggestedActions) { action ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE0F2FE),
                            border = BorderStroke(1.dp, AccentCyanBorder),
                            modifier = Modifier
                                .clickable { onExecuteAction(action) }
                                .testTag("action_${action.actionType.name}")
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
                                    text = action.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF0369A1),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }



            // Floating Capsule Message Composer (Anchored directly above soft keyboard)
            StudyBuddyComposer(
                inputText = inputText,
                isSending = isSending,
                attachedContextLabel = attachedContextLabel,
                attachedFiles = attachedFiles,
                onRemoveAttachment = {
                    attachedContextSnippet = null
                    attachedContextLabel = null
                },
                onRemoveFile = { fileToRemove ->
                    attachedFiles = attachedFiles.filterNot { it.fileName == fileToRemove.fileName }
                },
                onOpenFilePicker = {
                    try {
                        documentPickerLauncher.launch(
                            arrayOf(
                                "image/*",
                                "application/pdf",
                                "text/plain",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            )
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open document picker: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                onVoiceClick = { launchVoiceInput() },
                onStop = onStopGeneration,
                onInputChanged = { inputText = it },
                onSend = {
                    val text = inputText.trim()
                    if ((text.isNotEmpty() || attachedFiles.isNotEmpty()) && !isSending) {
                        val promptText = if (text.isEmpty() && attachedFiles.isNotEmpty()) {
                            "Please analyze the attached document / image for my study."
                        } else {
                            text
                        }
                        if (onSendMessageWithAttachments != null) {
                            onSendMessageWithAttachments(promptText, attachedContextSnippet, attachedFiles)
                        } else if (onSendMessageWithContext != null) {
                            onSendMessageWithContext(promptText, attachedContextSnippet)
                        } else {
                            val fullMsg = if (!attachedContextSnippet.isNullOrBlank()) {
                                "[$attachedContextSnippet]\n\n$promptText"
                            } else {
                                promptText
                            }
                            onSendMessage(fullMsg)
                        }
                        inputText = ""
                        attachedFiles = emptyList()
                        attachedContextSnippet = null
                        attachedContextLabel = null
                    }
                }
            )
        }
    }

    // History Bottom Sheet
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Conversations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkTextColor
                    )
                    TextButton(onClick = {
                        onStartNewChat()
                        showHistorySheet = false
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = UserBubbleCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Chat", color = UserBubbleCyan, fontWeight = FontWeight.SemiBold)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No previous conversations yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SubtitleColor
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            val isSelected = conv.id == currentConversationId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                border = if (isSelected) BorderStroke(1.5.dp, AccentCyanBorder) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectConversation(conv.id)
                                        showHistorySheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = conv.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = DarkTextColor
                                        )
                                        val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(conv.updatedAt))
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SubtitleColor
                                        )
                                    }
                                    IconButton(
                                        onClick = { conversationToDelete = conv },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Conversation",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    conversationToDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Delete Conversation?", color = DarkTextColor, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${conv.title}'? This action cannot be undone.", color = DarkTextColor) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConversation(conv.id)
                        conversationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancel", color = SubtitleColor)
                }
            }
        )
    }

    // Attachment Context Modal Bottom Sheet
    if (showAttachmentDialog) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentDialog = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Attach Study Context",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkTextColor
                )
                Text(
                    text = "Ground your prompt in active StudyHub syllabus and progress data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtitleColor
                )
                Spacer(modifier = Modifier.height(16.dp))

                val options = listOf(
                    Triple(
                        Icons.Default.School,
                        "Active Syllabus Topic",
                        if (activeTopicContext?.first != null) {
                            Pair(
                                "Subject: ${activeTopicContext.second ?: "General"} • Topic: ${activeTopicContext.first}",
                                "Topic Context: ${activeTopicContext.first} (${activeTopicContext.second ?: "General"})"
                            )
                        } else {
                            Pair(
                                "Active Study Curriculum & Board Track",
                                "Syllabus Context: Active curriculum and board syllabus track."
                            )
                        }
                    ),
                    Triple(
                        Icons.Default.Psychology,
                        "Due Revision Cards",
                        Pair(
                            "${revisionMetrics?.dueTodayCount ?: 0} cards due today (${revisionMetrics?.overdueCount ?: 0} overdue)",
                            "Revision Context: Student has ${revisionMetrics?.dueTodayCount ?: 0} cards due today (${revisionMetrics?.overdueCount ?: 0} overdue)."
                        )
                    ),
                    Triple(
                        Icons.Default.EventNote,
                        "Daily Study Schedule",
                        Pair(
                            "Goal: ${userProfile?.dailyGoalMinutes ?: 120} min target per day",
                            "Schedule Context: Student daily study target is ${userProfile?.dailyGoalMinutes ?: 120} minutes."
                        )
                    ),
                    Triple(
                        Icons.Default.TrackChanges,
                        "Target Track & Goals",
                        Pair(
                            "Track: ${userProfile?.admissionTrack ?: userProfile?.academicLevel ?: "HSC / Admission"}",
                            "Goal Context: Student is targeting ${userProfile?.admissionTrack ?: userProfile?.academicLevel ?: "HSC / Admission"} in target year ${userProfile?.targetYear ?: "2025"}."
                        )
                    )
                )

                options.forEach { (icon, title, details) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                attachedContextLabel = title
                                attachedContextSnippet = details.second
                                showAttachmentDialog = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = UserBubbleCyan, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = DarkTextColor)
                                Text(details.first, style = MaterialTheme.typography.labelSmall, color = SubtitleColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Screen A: Premium Welcome Screen matching exact reference layout.
 * Displays Mascot Hero, Greeting, Grounded Study Context Card, and 4 Suggested Questions.
 */
@Composable
private fun EmptyBuddyWelcomeState(
    studentName: String?,
    revisionMetrics: RevisionSummaryMetrics?,
    enabledContextCount: Int = 8,
    onOpenContextSheet: () -> Unit = {},
    onSelectPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Mascot Hero
        StudyBuddyEmblem(size = EmblemSize.HERO, isAnimated = true)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (!studentName.isNullOrBlank()) "Hello $studentName!" else "Hello Student!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = DarkTextColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Your AI Study Companion — grounded in your actual StudyHub syllabus & study progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = SubtitleColor,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Grounded "Your Study Context" Card (Wired to Real Database Metrics)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, AssistantCardBorder),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenContextSheet() }
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = UserBubbleCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connected Study Context",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DarkTextColor
                        )
                    }

                    Text(
                        text = "Auto-Synced",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF0369A1),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val totalTopics = revisionMetrics?.totalTrackedCount ?: 0
                val dueCount = (revisionMetrics?.dueTodayCount ?: 0) + (revisionMetrics?.overdueCount ?: 0)
                val retentionRate = revisionMetrics?.onTimeCompletionRatePercent ?: 100

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ContextMetricItem(
                        icon = "📚",
                        value = if (totalTopics > 0) "$totalTopics" else "Active",
                        label = "Syllabus Topics",
                        modifier = Modifier.weight(1f)
                    )
                    ContextMetricItem(
                        icon = "📋",
                        value = if (dueCount > 0) "$dueCount Due" else "Up to date",
                        label = "Pending Reviews",
                        modifier = Modifier.weight(1f)
                    )
                    ContextMetricItem(
                        icon = "📊",
                        value = "$retentionRate%",
                        label = "Retention Rate",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Suggested Questions Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Suggested Questions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = DarkTextColor
            )
        }

        val dueCount = (revisionMetrics?.dueTodayCount ?: 0) + (revisionMetrics?.overdueCount ?: 0)

        val promptSuggestions = listOf(
            PromptSuggestion(
                title = "Revise Due Topics",
                description = if (dueCount > 0) "Help me revise my $dueCount due topics with active recall" else "Help me revise active syllabus chapters",
                prompt = if (dueCount > 0) "Help me revise my $dueCount due topics" else "What should I revise today?"
            ),
            PromptSuggestion(
                title = "Daily Study Schedule",
                description = "Plan an optimal study schedule for today based on my scheduled tasks and deadlines",
                prompt = "Help me plan my study schedule for today"
            ),
            PromptSuggestion(
                title = "Syllabus Progress",
                description = "Explain my overall progress and syllabus completion breakdown",
                prompt = "Explain my progress and syllabus completion"
            ),
            PromptSuggestion(
                title = "Next Target Study",
                description = "Recommend what chapter or subject to focus on next for admission prep",
                prompt = "What should I study next?"
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            promptSuggestions.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, AssistantCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPrompt(item.prompt) }
                        .testTag("welcome_suggestion_${item.prompt}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = UserBubbleCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = SubtitleColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ContextMetricItem(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = UserBubbleCyan
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SubtitleColor,
            textAlign = TextAlign.Center
        )
    }
}

private data class PromptSuggestion(
    val title: String,
    val description: String,
    val prompt: String
)

/**
 * Screen B: Active Conversation Bubble.
 * Renders User messages (Cyan pill right-aligned) and Assistant messages
 * with native Markdown typography, headings, bold text, lists, and code blocks.
 */
@Composable
private fun BuddyMessageBubble(
    message: BuddyMessageEntity,
    onExecuteAction: (BuddyAction) -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onAddPlanToCalendar: (String, Int, String, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onSaveToRevision: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onCopy: () -> Unit,
    onRetry: () -> Unit
) {
    val isUser = message.sender == "USER"
    val isFailed = message.status == "FAILED"

    val artifact = remember(message.content) {
        if (!isUser) StudyBuddyArtifactParser.parse(message.content) else null
    }
    val isWideArtifact = remember(artifact) {
        artifact?.blocks?.any {
            it is BuddyBlock.StudyPlanBlock ||
            it is BuddyBlock.QuizBlock ||
            it is BuddyBlock.StepByStepBlock ||
            it is BuddyBlock.FormulaBlock ||
            it is BuddyBlock.FlashcardBlock ||
            it is BuddyBlock.ConceptMapBlock ||
            it is BuddyBlock.WarningBlock
        } ?: false
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            StudyBuddyEmblem(size = EmblemSize.SMALL)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = when {
                isFailed -> Color(0xFFFEF2F2)
                isUser -> UserBubbleCyan
                else -> AssistantCardBg
            },
            border = when {
                isFailed -> BorderStroke(1.dp, Color(0xFFFCA5A5))
                isUser -> null
                else -> BorderStroke(1.dp, AssistantCardBorder)
            },
            shadowElevation = if (!isUser && !isFailed) 1.dp else 0.dp,
            modifier = Modifier
                .then(
                    if (isUser) Modifier.widthIn(min = 60.dp, max = 290.dp)
                    else Modifier.weight(1f, fill = false)
                )
                .testTag("bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(if (isUser) 12.dp else 14.dp)) {
                if (isUser) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = Color.White
                    )
                } else {
                    artifact?.let { art ->
                        StudyBuddyArtifactRenderer(
                            artifact = art,
                            onExecuteAction = onExecuteAction,
                            onSelectFollowUp = onSendMessage,
                            onAddPlanToCalendar = onAddPlanToCalendar,
                            onSaveToRevision = onSaveToRevision
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom timestamp and message actions
                Row(
                    modifier = if (isWideArtifact) Modifier.fillMaxWidth() else Modifier.wrapContentWidth(),
                    horizontalArrangement = if (isWideArtifact) Arrangement.SpaceBetween else Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.createdAt))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) Color.White.copy(alpha = 0.75f) else SubtitleColor,
                        fontSize = 10.sp
                    )

                    if (!isUser) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isFailed) {
                                TextButton(
                                    onClick = onRetry,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFDC2626))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retry", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626))
                                }
                            } else {
                                IconButton(
                                    onClick = onCopy,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Response",
                                        tint = SubtitleColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onSendMessage("Please check this answer and review the steps for accuracy.")
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Check Solution",
                                        tint = SubtitleColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onRetry,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Regenerate",
                                        tint = SubtitleColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Truthful, dynamic AI activity indicator rendered inside the conversation.
 * No redundant stop button; screen-reader accessible via polite live region.
 */
@Composable
private fun BuddyThinkingIndicator(
    activityText: String = "Preparing your response"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.Start
    ) {
        StudyBuddyEmblem(size = EmblemSize.SMALL, isAnimated = true)
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, AssistantCardBorder),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = UserBubbleCyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = activityText,
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtitleColor
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    return if (bytes < 1024 * 1024) {
        "${bytes / 1024} KB"
    } else {
        String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
    }
}

/**
 * Floating Capsule Message Composer (Anchored directly above soft keyboard).
 * Includes device file attachment chips, (+) file/photo picker button,
 * voice dictation mic, and dedicated primary Stop control during generation.
 */
@Composable
private fun StudyBuddyComposer(
    inputText: String,
    isSending: Boolean,
    attachedContextLabel: String? = null,
    attachedFiles: List<AttachedFileData> = emptyList(),
    onRemoveAttachment: () -> Unit = {},
    onRemoveFile: (AttachedFileData) -> Unit = {},
    onOpenFilePicker: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onStop: () -> Unit = {},
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = SoftBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Attached context chip
            if (!attachedContextLabel.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, AccentCyanBorder),
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = UserBubbleCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = attachedContextLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF0369A1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove study context",
                            tint = Color(0xFF0369A1),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemoveAttachment() }
                        )
                    }
                }
            }

            // Attached device files chips
            if (attachedFiles.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp, start = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(attachedFiles, key = { it.fileName }) { file ->
                        val isImage = file.mimeType.startsWith("image/")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.testTag("attached_file_${file.fileName}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isImage) Icons.Default.Image else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = file.fileName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF15803D),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 140.dp)
                                )
                                val sizeStr = formatFileSize(file.sizeBytes)
                                if (sizeStr.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "($sizeStr)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF15803D).copy(alpha = 0.75f),
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove ${file.fileName}",
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemoveFile(file) }
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, AccentCyanBorder),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenFilePicker,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("buddy_attachment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach file or photo",
                            tint = UserBubbleCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputChanged,
                        placeholder = {
                            Text(
                                text = "Ask Study Buddy...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SubtitleColor
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("buddy_input_field"),
                        minLines = 1,
                        maxLines = 4,
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("buddy_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice input",
                            tint = SubtitleColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    if (isSending) {
                        // Light-blue primary cancellation control matching StudyHub design system
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7))
                                .testTag("buddy_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop generating",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        val canSend = inputText.trim().isNotEmpty() || attachedFiles.isNotEmpty()
                        IconButton(
                            onClick = onSend,
                            enabled = canSend,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSend) UserBubbleCyan
                                    else Color(0xFFCBD5E1)
                                )
                                .testTag("buddy_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
