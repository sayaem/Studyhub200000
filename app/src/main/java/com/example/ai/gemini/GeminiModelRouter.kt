package com.example.ai.gemini

enum class GeminiTaskCategory {
    MATHEMATICAL_PROBLEM_SOLVING,
    SCIENCE_EXPLANATION,
    PROGRAMMING_DEBUGGING,
    QUIZ_GENERATION,
    STUDY_PLANNING,
    REVISION_ANALYSIS,
    FACTUAL_LOOKUP,
    GENERAL_TUTORING
}

data class ModelPlan(
    val primaryModel: String,
    val fallbackModels: List<String>,
    val temperature: Double,
    val maxOutputTokens: Int,
    val taskCategory: GeminiTaskCategory
)

object GeminiModelRouter {

    // Active models conforming to current Gemini REST API endpoints
    private const val MODEL_PRO_REASONING = "gemini-3.1-pro-preview"
    private const val MODEL_FLASH_BALANCED = "gemini-3.5-flash"
    private const val MODEL_FLASH_LATEST = "gemini-3.8-flash"
    private const val MODEL_FLASH_LITE = "gemini-flash-latest"

    /**
     * Determines the optimal model and generation parameters based on prompt content and context.
     */
    fun route(prompt: String, maxTokensConfig: Int = 4096): ModelPlan {
        val category = classifyTask(prompt)

        return when (category) {
            GeminiTaskCategory.MATHEMATICAL_PROBLEM_SOLVING -> ModelPlan(
                primaryModel = MODEL_PRO_REASONING,
                fallbackModels = listOf(MODEL_FLASH_BALANCED, MODEL_FLASH_LATEST),
                temperature = 0.2, // Low temperature for mathematical precision
                maxOutputTokens = maxTokensConfig.coerceAtLeast(3072),
                taskCategory = category
            )

            GeminiTaskCategory.SCIENCE_EXPLANATION -> ModelPlan(
                primaryModel = MODEL_PRO_REASONING,
                fallbackModels = listOf(MODEL_FLASH_BALANCED, MODEL_FLASH_LATEST),
                temperature = 0.3,
                maxOutputTokens = maxTokensConfig.coerceAtLeast(2048),
                taskCategory = category
            )

            GeminiTaskCategory.PROGRAMMING_DEBUGGING -> ModelPlan(
                primaryModel = MODEL_PRO_REASONING,
                fallbackModels = listOf(MODEL_FLASH_BALANCED, MODEL_FLASH_LATEST),
                temperature = 0.2,
                maxOutputTokens = maxTokensConfig.coerceAtLeast(2048),
                taskCategory = category
            )

            GeminiTaskCategory.QUIZ_GENERATION -> ModelPlan(
                primaryModel = MODEL_FLASH_BALANCED,
                fallbackModels = listOf(MODEL_FLASH_LATEST, MODEL_PRO_REASONING),
                temperature = 0.4,
                maxOutputTokens = 2048,
                taskCategory = category
            )

            GeminiTaskCategory.STUDY_PLANNING -> ModelPlan(
                primaryModel = MODEL_FLASH_BALANCED,
                fallbackModels = listOf(MODEL_FLASH_LATEST, MODEL_FLASH_LITE),
                temperature = 0.5,
                maxOutputTokens = 2048,
                taskCategory = category
            )

            GeminiTaskCategory.REVISION_ANALYSIS -> ModelPlan(
                primaryModel = MODEL_FLASH_BALANCED,
                fallbackModels = listOf(MODEL_FLASH_LATEST, MODEL_FLASH_LITE),
                temperature = 0.4,
                maxOutputTokens = 2048,
                taskCategory = category
            )

            GeminiTaskCategory.FACTUAL_LOOKUP -> ModelPlan(
                primaryModel = MODEL_FLASH_BALANCED,
                fallbackModels = listOf(MODEL_FLASH_LITE, MODEL_FLASH_LATEST),
                temperature = 0.3,
                maxOutputTokens = 1024,
                taskCategory = category
            )

            GeminiTaskCategory.GENERAL_TUTORING -> ModelPlan(
                primaryModel = MODEL_FLASH_BALANCED,
                fallbackModels = listOf(MODEL_FLASH_LATEST, MODEL_PRO_REASONING),
                temperature = 0.7,
                maxOutputTokens = maxTokensConfig,
                taskCategory = category
            )
        }
    }

    /**
     * Fast, deterministic local classifier requiring 0ms network latency.
     */
    fun classifyTask(prompt: String): GeminiTaskCategory {
        val lower = prompt.lowercase()

        // 1. Quiz keywords (explicit structured quiz request)
        if (lower.contains("quiz") || lower.contains("mcq") ||
            lower.contains("test me") || lower.contains("practice question") ||
            lower.contains("multiple choice") || lower.contains("কুইজ") || lower.contains("প্রশ্ন")
        ) {
            return GeminiTaskCategory.QUIZ_GENERATION
        }

        // 2. Study planning keywords (explicit schedule or routine request)
        if (lower.contains("routine") || lower.contains("plan") ||
            lower.contains("schedule") || lower.contains("timetable") ||
            lower.contains("calendar") || lower.contains("study plan") ||
            lower.contains("পরিকল্পনা") || lower.contains("রুটিন")
        ) {
            return GeminiTaskCategory.STUDY_PLANNING
        }

        // 3. Revision keywords (explicit recall or spaced repetition review)
        if (lower.contains("revision") || lower.contains("review") ||
            lower.contains("recall") || lower.contains("flashcard") ||
            lower.contains("রিভিশন") || lower.contains("স্মরণ")
        ) {
            return GeminiTaskCategory.REVISION_ANALYSIS
        }

        // 4. Math keywords & formulas
        if (lower.contains("integrate") || lower.contains("derivative") ||
            lower.contains("equation") || lower.contains("calculate") ||
            lower.contains("solve") || lower.contains("formula") ||
            lower.contains("vector") || lower.contains("matrix") ||
            lower.contains("algebra") || lower.contains("geometry") ||
            lower.contains("trigonometry") || lower.contains("sin(") ||
            lower.contains("cos(") || lower.contains("tan(") ||
            lower.contains("log(") || lower.contains("\\(") ||
            lower.contains("dx") || lower.contains("theorem") ||
            lower.contains("গণিত") || lower.contains("সমাধান") || lower.contains("মান নির্ণয়")
        ) {
            return GeminiTaskCategory.MATHEMATICAL_PROBLEM_SOLVING
        }

        // 5. Science keywords
        if (lower.contains("physics") || lower.contains("chemistry") ||
            lower.contains("biology") || lower.contains("reaction") ||
            lower.contains("molecule") || lower.contains("velocity") ||
            lower.contains("force") || lower.contains("acceleration") ||
            lower.contains("energy") || lower.contains("electric") ||
            lower.contains("magnetic") || lower.contains("gravity") ||
            lower.contains("পদার্থ") || lower.contains("রসায়ন") || lower.contains("জীববিজ্ঞান")
        ) {
            return GeminiTaskCategory.SCIENCE_EXPLANATION
        }

        // 6. Programming keywords
        if (lower.contains("kotlin") || lower.contains("java") ||
            lower.contains("python") || lower.contains("code") ||
            lower.contains("function") || lower.contains("bug") ||
            lower.contains("algorithm") || lower.contains("debug") ||
            lower.contains("compile") || lower.contains("class")
        ) {
            return GeminiTaskCategory.PROGRAMMING_DEBUGGING
        }

        // 7. Factual lookup
        if (lower.startsWith("what is") || lower.startsWith("who is") ||
            lower.startsWith("when did") || lower.startsWith("define") ||
            lower.length < 30
        ) {
            return GeminiTaskCategory.FACTUAL_LOOKUP
        }

        return GeminiTaskCategory.GENERAL_TUTORING
    }
}
