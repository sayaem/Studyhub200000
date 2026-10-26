package com.example.buddy

import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Robust, publication-quality parser that extracts and validates structured Study Buddy artifacts from Gemini output.
 * If standard JSON parsing fails due to unescaped LaTeX, streaming truncation, or syntax anomalies,
 * it performs lenient regex recovery and JSON syntax sanitization so raw JSON is NEVER shown to students.
 */
object StudyBuddyArtifactParser {

    fun parse(rawContent: String): BuddyArtifactResponse {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) {
            return fallbackToText("")
        }

        val jsonStr = extractJsonString(trimmed)
        if (jsonStr != null) {
            try {
                val sanitized = sanitizeJsonEscapes(jsonStr)
                val json = JSONObject(sanitized)
                val summary = json.optString("summary").ifBlank { null }
                val blocksArray = json.optJSONArray("blocks")

                if (blocksArray != null && blocksArray.length() > 0) {
                    val blocks = mutableListOf<BuddyBlock>()
                    for (i in 0 until blocksArray.length()) {
                        val blockObj = blocksArray.optJSONObject(i) ?: continue
                        val block = parseBlock(blockObj)
                        if (block != null) {
                            blocks.add(block)
                        }
                    }

                    if (blocks.isNotEmpty()) {
                        return BuddyArtifactResponse(summary = summary, blocks = blocks)
                    }
                } else if (!summary.isNullOrBlank()) {
                    return BuddyArtifactResponse(summary = summary, blocks = listOf(BuddyBlock.TextBlock(summary)))
                }
            } catch (_: Exception) {
                // Fall through to lenient recovery
            }
        }

        // Lenient recovery pass for malformed / truncated / fenced JSON
        return lenientJsonRecovery(trimmed)
    }

    private fun extractJsonString(text: String): String? {
        val trimmed = text.trim()

        // 1. Check for markdown code fences (e.g. ```json ... ``` or unclosed ```json ...)
        if (trimmed.contains("```json")) {
            val start = trimmed.indexOf("```json") + 7
            val end = trimmed.indexOf("```", start)
            val candidate = if (end > start) {
                trimmed.substring(start, end).trim()
            } else {
                trimmed.substring(start).trim()
            }
            if (candidate.startsWith("{")) return candidate
        } else if (trimmed.contains("```")) {
            val start = trimmed.indexOf("```") + 3
            val end = trimmed.indexOf("```", start)
            val candidate = if (end > start) {
                trimmed.substring(start, end).trim()
            } else {
                trimmed.substring(start).trim()
            }
            if (candidate.startsWith("{")) return candidate
        }

        // 2. Locate outermost JSON object boundaries { ... }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        } else if (firstBrace != -1) {
            // Truncated trailing JSON object
            return trimmed.substring(firstBrace)
        }

        return null
    }

    /**
     * Sanitizes unescaped backslashes commonly emitted by LLMs in LaTeX strings (e.g. \(, \vec, \alpha, \theta)
     * so that standard org.json parser can safely process the JSON without throwing syntax errors.
     */
    private fun sanitizeJsonEscapes(json: String): String {
        val sb = StringBuilder()
        var inString = false
        var i = 0
        while (i < json.length) {
            val c = json[i]
            if (!inString) {
                if (c == '"') {
                    inString = true
                }
                sb.append(c)
                i++
            } else {
                if (c == '"') {
                    inString = false
                    sb.append(c)
                    i++
                } else if (c == '\\') {
                    if (i + 1 < json.length) {
                        val next = json[i + 1]
                        when (next) {
                            '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> {
                                sb.append('\\')
                                sb.append(next)
                                i += 2
                            }
                            'u' -> {
                                if (i + 5 < json.length && json.substring(i + 2, i + 6).all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                                    sb.append(json.substring(i, i + 6))
                                    i += 6
                                } else {
                                    sb.append("\\\\")
                                    i++
                                }
                            }
                            else -> {
                                // Escape invalid backslash for valid JSON parsing
                                sb.append("\\\\")
                                i++
                            }
                        }
                    } else {
                        sb.append("\\\\")
                        i++
                    }
                } else {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun parseBlock(obj: JSONObject): BuddyBlock? {
        val type = obj.optString("type", "").lowercase()
        return when (type) {
            "text" -> {
                val content = obj.optString("content")
                if (content.isNotBlank()) BuddyBlock.TextBlock(content) else null
            }
            "insight" -> {
                val title = obj.optString("title", "Key Insight")
                val text = obj.optString("text")
                val level = obj.optString("level", "HIGHLIGHT").uppercase()
                if (text.isNotBlank()) BuddyBlock.InsightBlock(title, text, level) else null
            }
            "stat_card", "stats" -> {
                val title = obj.optString("title").ifBlank { null }
                val metricsArray = obj.optJSONArray("metrics") ?: JSONArray()
                val metrics = mutableListOf<StatMetric>()
                for (i in 0 until metricsArray.length()) {
                    val mObj = metricsArray.optJSONObject(i) ?: continue
                    val label = mObj.optString("label")
                    val value = mObj.optString("value")
                    val sublabel = mObj.optString("sublabel").ifBlank { null }
                    val icon = mObj.optString("icon").ifBlank { null }
                    if (label.isNotBlank() && value.isNotBlank()) {
                        metrics.add(StatMetric(label, value, sublabel, icon))
                    }
                }
                if (metrics.isNotEmpty()) BuddyBlock.StatCardBlock(title, metrics) else null
            }
            "study_plan", "plan" -> {
                val title = obj.optString("title", "Study Plan")
                val dateDesc = obj.optString("dateDescription").ifBlank { null }
                val stepsArray = obj.optJSONArray("steps") ?: JSONArray()
                val steps = mutableListOf<StudyPlanStep>()
                for (i in 0 until stepsArray.length()) {
                    val sObj = stepsArray.optJSONObject(i) ?: continue
                    val stepTitle = sObj.optString("title")
                    val time = sObj.optString("time").ifBlank { null }
                    val subject = sObj.optString("subject").ifBlank { null }
                    val duration = sObj.optInt("durationMinutes", 0).let { if (it > 0) it else null }
                    val completed = sObj.optBoolean("isCompleted", false)
                    if (stepTitle.isNotBlank()) {
                        steps.add(StudyPlanStep(time, stepTitle, subject, duration, completed))
                    }
                }
                if (steps.isNotEmpty()) BuddyBlock.StudyPlanBlock(title, dateDesc, steps) else null
            }
            "checklist" -> {
                val title = obj.optString("title", "Checklist")
                val itemsArray = obj.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<ChecklistItem>()
                for (i in 0 until itemsArray.length()) {
                    val iObj = itemsArray.optJSONObject(i) ?: continue
                    val id = iObj.optString("id", "chk_$i")
                    val itemText = iObj.optString("text")
                    val isDone = iObj.optBoolean("isDone", false)
                    if (itemText.isNotBlank()) {
                        items.add(ChecklistItem(id, itemText, isDone))
                    }
                }
                if (items.isNotEmpty()) BuddyBlock.ChecklistBlock(title, items) else null
            }
            "quiz" -> {
                val question = obj.optString("question")
                val optionsArray = obj.optJSONArray("options") ?: JSONArray()
                val options = mutableListOf<String>()
                for (i in 0 until optionsArray.length()) {
                    val opt = optionsArray.optString(i)
                    if (opt.isNotBlank()) options.add(opt)
                }
                val rawCorrect = obj.optInt("correctIndex", 0)
                val safeCorrect = if (options.isNotEmpty()) rawCorrect.coerceIn(0, options.size - 1) else 0
                val explanation = obj.optString("explanation", "Review the formula and core concepts in your syllabus.")
                if (question.isNotBlank() && options.size >= 2) {
                    BuddyBlock.QuizBlock(question, options, safeCorrect, explanation)
                } else null
            }
            "expandable_detail", "expandable" -> {
                val title = obj.optString("title", "More Details")
                val content = obj.optString("content")
                if (content.isNotBlank()) BuddyBlock.ExpandableBlock(title, content) else null
            }
            "action_group", "actions" -> {
                val actionsArray = obj.optJSONArray("actions") ?: JSONArray()
                val actions = mutableListOf<BuddyArtifactAction>()
                for (i in 0 until actionsArray.length()) {
                    val aObj = actionsArray.optJSONObject(i) ?: continue
                    val actionType = aObj.optString("actionType")
                    val title = aObj.optString("title")
                    val desc = aObj.optString("description").ifBlank { null }
                    val targetSubId = aObj.optLong("targetSubjectId", -1L).let { if (it > 0) it else null }
                    val targetChapId = aObj.optLong("targetChapterId", -1L).let { if (it > 0) it else null }
                    val date = aObj.optString("scheduledDate").ifBlank { null }
                    val duration = aObj.optInt("durationMins", 0).let { if (it > 0) it else null }
                    if (actionType.isNotBlank() && title.isNotBlank()) {
                        actions.add(BuddyArtifactAction(actionType, title, desc, targetSubId, targetChapId, date, duration))
                    }
                }
                if (actions.isNotEmpty()) BuddyBlock.ActionGroupBlock(actions) else null
            }
            "follow_up", "suggestions" -> {
                val suggestionsArray = obj.optJSONArray("suggestions") ?: JSONArray()
                val suggestions = mutableListOf<String>()
                for (i in 0 until suggestionsArray.length()) {
                    val sug = suggestionsArray.optString(i)
                    if (sug.isNotBlank()) suggestions.add(sug)
                }
                if (suggestions.isNotEmpty()) BuddyBlock.FollowUpBlock(suggestions) else null
            }
            "step_by_step", "steps", "solution" -> {
                val title = obj.optString("title", "Step-by-Step Solution")
                val stepsArray = obj.optJSONArray("steps") ?: JSONArray()
                val steps = mutableListOf<StepItem>()
                for (i in 0 until stepsArray.length()) {
                    val sObj = stepsArray.optJSONObject(i) ?: continue
                    val stepNum = sObj.optInt("stepNumber", i + 1)
                    val sTitle = sObj.optString("title", "Step $stepNum")
                    val content = sObj.optString("content")
                    val crucial = sObj.optBoolean("isCrucial", false)
                    if (content.isNotBlank()) {
                        steps.add(StepItem(stepNum, sTitle, content, crucial))
                    }
                }
                val keyTakeaway = obj.optString("keyTakeaway").ifBlank { null }
                if (steps.isNotEmpty()) BuddyBlock.StepByStepBlock(title, steps, keyTakeaway) else null
            }
            "formula_card", "formula", "equation" -> {
                val title = obj.optString("title", "Key Formula")
                val latex = obj.optString("formulaLatex", obj.optString("formula"))
                val variablesArray = obj.optJSONArray("variables") ?: JSONArray()
                val variables = mutableListOf<FormulaVariable>()
                for (i in 0 until variablesArray.length()) {
                    val vObj = variablesArray.optJSONObject(i) ?: continue
                    val sym = vObj.optString("symbol")
                    val name = vObj.optString("name")
                    val unit = vObj.optString("unit").ifBlank { null }
                    if (sym.isNotBlank() && name.isNotBlank()) {
                        variables.add(FormulaVariable(sym, name, unit))
                    }
                }
                val notes = obj.optString("notes").ifBlank { null }
                val example = obj.optString("example").ifBlank { null }
                if (latex.isNotBlank()) BuddyBlock.FormulaBlock(title, latex, variables, notes, example) else null
            }
            "flashcard", "card", "recall" -> {
                val front = obj.optString("front", obj.optString("question"))
                val back = obj.optString("back", obj.optString("answer"))
                val tag = obj.optString("tag").ifBlank { null }
                val hint = obj.optString("hint").ifBlank { null }
                if (front.isNotBlank() && back.isNotBlank()) BuddyBlock.FlashcardBlock(front, back, tag, hint) else null
            }
            "concept_map", "mindmap", "map" -> {
                val topic = obj.optString("topic", "Concept Map")
                val nodesArray = obj.optJSONArray("nodes") ?: JSONArray()
                val nodes = mutableListOf<ConceptNode>()
                for (i in 0 until nodesArray.length()) {
                    val nObj = nodesArray.optJSONObject(i) ?: continue
                    val name = nObj.optString("name")
                    val desc = nObj.optString("description")
                    val level = nObj.optInt("level", 0)
                    if (name.isNotBlank()) {
                        nodes.add(ConceptNode(name, desc, level))
                    }
                }
                val connsArray = obj.optJSONArray("connections") ?: JSONArray()
                val conns = mutableListOf<String>()
                for (i in 0 until connsArray.length()) {
                    val c = connsArray.optString(i)
                    if (c.isNotBlank()) conns.add(c)
                }
                if (nodes.isNotEmpty()) BuddyBlock.ConceptMapBlock(topic, nodes, conns) else null
            }
            "warning", "trap", "common_mistakes" -> {
                val title = obj.optString("title", "Common Exam Pitfall")
                val caution = obj.optString("cautionText", obj.optString("text"))
                val mistake = obj.optString("commonMistake").ifBlank { null }
                val approach = obj.optString("correctApproach").ifBlank { null }
                if (caution.isNotBlank()) BuddyBlock.WarningBlock(title, caution, mistake, approach) else null
            }
            else -> null
        }
    }

    /**
     * Lenient recovery pass that uses regex heuristics to extract summary, quiz, and insight blocks
     * from malformed or truncated JSON strings. If regex recovery fails, it sanitizes JSON syntax completely.
     */
    private fun lenientJsonRecovery(text: String): BuddyArtifactResponse {
        val extractedBlocks = mutableListOf<BuddyBlock>()

        // 1. Extract summary field if present
        val summaryPattern = Pattern.compile("(?i)\"summary\"\\s*:\\s*\"([^\"]*)\"")
        val summaryMatcher = summaryPattern.matcher(text)
        val extractedSummary = if (summaryMatcher.find()) summaryMatcher.group(1) else null

        // 2. Extract Quiz blocks via Regex
        val quizQuestionPattern = Pattern.compile("(?i)\"question\"\\s*:\\s*\"([^\"]*)\"")
        val quizOptPattern = Pattern.compile("(?i)\"options\"\\s*:\\s*\\[([^\\]]*)\\]")
        val quizExpPattern = Pattern.compile("(?i)\"explanation\"\\s*:\\s*\"([^\"]*)\"")

        val qMatcher = quizQuestionPattern.matcher(text)
        val optMatcher = quizOptPattern.matcher(text)
        if (qMatcher.find() && optMatcher.find()) {
            val question = qMatcher.group(1) ?: ""
            val rawOpts = optMatcher.group(1) ?: ""
            val options = rawOpts.split(",").map { it.trim().removeSurrounding("\"").removeSurrounding("'") }.filter { it.isNotBlank() }
            val explanation = if (quizExpPattern.matcher(text).find()) {
                val expM = quizExpPattern.matcher(text)
                expM.find()
                expM.group(1) ?: "Check concept explanation."
            } else "Check concept explanation."

            if (question.isNotBlank() && options.size >= 2) {
                extractedBlocks.add(BuddyBlock.QuizBlock(question, options, 0, explanation))
            }
        }

        // 3. Extract Insight blocks via Regex
        val insightTitlePattern = Pattern.compile("(?i)\"title\"\\s*:\\s*\"([^\"]*)\"")
        val insightTextPattern = Pattern.compile("(?i)\"text\"\\s*:\\s*\"([^\"]*)\"")
        val tMatcher = insightTitlePattern.matcher(text)
        val textMatcher = insightTextPattern.matcher(text)
        if (tMatcher.find() && textMatcher.find()) {
            val title = tMatcher.group(1) ?: "Key Insight"
            val body = textMatcher.group(1) ?: ""
            if (body.isNotBlank()) {
                extractedBlocks.add(BuddyBlock.InsightBlock(title, body, "HIGHLIGHT"))
            }
        }

        // 4. Extract Formula blocks via Regex
        val formulaPattern = Pattern.compile("(?i)\"(?:formulaLatex|formula)\"\\s*:\\s*\"([^\"]*)\"")
        val formulaMatcher = formulaPattern.matcher(text)
        if (formulaMatcher.find()) {
            val fLatex = formulaMatcher.group(1) ?: ""
            if (fLatex.isNotBlank()) {
                val fTitle = if (tMatcher.find()) tMatcher.group(1) ?: "Key Formula" else "Key Formula"
                extractedBlocks.add(BuddyBlock.FormulaBlock(title = fTitle, formulaLatex = fLatex))
            }
        }

        // 5. Extract Flashcard via Regex
        val frontPattern = Pattern.compile("(?i)\"(?:front|question)\"\\s*:\\s*\"([^\"]*)\"")
        val backPattern = Pattern.compile("(?i)\"(?:back|answer)\"\\s*:\\s*\"([^\"]*)\"")
        val frontM = frontPattern.matcher(text)
        val backM = backPattern.matcher(text)
        if (frontM.find() && backM.find()) {
            val front = frontM.group(1) ?: ""
            val back = backM.group(1) ?: ""
            if (front.isNotBlank() && back.isNotBlank() && extractedBlocks.none { it is BuddyBlock.QuizBlock }) {
                extractedBlocks.add(BuddyBlock.FlashcardBlock(front = front, back = back))
            }
        }

        if (extractedBlocks.isNotEmpty()) {
            return BuddyArtifactResponse(summary = extractedSummary, blocks = extractedBlocks)
        }

        // 6. Fallback: sanitize JSON structural characters so raw JSON syntax is NEVER shown to the user
        val cleanedText = stripJsonSyntax(text)
        return BuddyArtifactResponse(
            summary = null,
            blocks = listOf(BuddyBlock.TextBlock(cleanedText))
        )
    }

    /**
     * Strips JSON structural syntax (braces, quotes, property keys like "summary":, "blocks":, etc.)
     * to render clean, readable prose text when unparseable JSON is encountered.
     */
    private fun stripJsonSyntax(text: String): String {
        var cleaned = text.trim()

        // Strip code fences
        cleaned = cleaned.replace(Regex("```(?:json)?"), "")

        // If the text starts with { and ends with }, strip key markers
        if (cleaned.contains("{") || cleaned.contains("}")) {
            cleaned = cleaned.replace(Regex("(?i)\"(?:summary|blocks|type|content|title|text|level|question|options|correctIndex|explanation|formulaLatex|formula|front|back|cautionText|commonMistake|correctApproach|keyTakeaway|stepNumber)\"\\s*:"), "")
            cleaned = cleaned.replace(Regex("[{}\\[\\]]"), " ")
            cleaned = cleaned.replace(Regex("\""), "")
            cleaned = cleaned.replace(Regex(",\\s*\\n"), "\n")
            cleaned = cleaned.replace(Regex("\\n\\s*\\n+"), "\n\n")
        }

        return cleaned.trim().ifBlank { "Here is the information requested." }
    }

    private fun fallbackToText(text: String): BuddyArtifactResponse {
        val cleaned = stripJsonSyntax(text)
        return BuddyArtifactResponse(
            summary = null,
            blocks = listOf(BuddyBlock.TextBlock(cleaned))
        )
    }
}

