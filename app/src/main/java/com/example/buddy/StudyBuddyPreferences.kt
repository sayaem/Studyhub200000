package com.example.buddy

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages local persistence of student-controlled context preferences.
 */
class StudyBuddyPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "studyhub_buddy_preferences"
        private const val KEY_ENABLED_CATEGORIES = "key_enabled_context_categories"
    }

    /**
     * Retrieves the set of enabled context categories. Defaults to all 8 categories.
     */
    fun getEnabledCategories(): Set<BuddyContextCategory> {
        val raw = prefs.getString(KEY_ENABLED_CATEGORIES, null) ?: return BuddyContextCategory.entries.toSet()
        if (raw.isBlank()) return emptySet()
        val ids = raw.split(",").map { it.trim() }.toSet()
        val found = BuddyContextCategory.entries.filter { it.id in ids }.toSet()
        return if (found.isEmpty() && raw != "NONE") BuddyContextCategory.entries.toSet() else found
    }

    /**
     * Persists the set of enabled context categories.
     */
    fun saveEnabledCategories(categories: Set<BuddyContextCategory>) {
        val raw = if (categories.isEmpty()) "NONE" else categories.joinToString(",") { it.id }
        prefs.edit().putString(KEY_ENABLED_CATEGORIES, raw).apply()
    }
}
