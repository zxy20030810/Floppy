package com.floppy.app.data

import android.content.Context
import android.content.SharedPreferences
import com.floppy.app.domain.UserProfile
import java.util.UUID

class LocalProfileStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getOrCreateUserId(): String {
        val existing = preferences.getString(KEY_USER_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val userId = "android_${UUID.randomUUID()}"
        preferences.edit()
            .putString(KEY_USER_ID, userId)
            .apply()
        return userId
    }

    fun readProfile(): UserProfile? {
        if (!preferences.getBoolean(KEY_HAS_PROFILE, false)) return null

        val fallback = UserProfile()
        return UserProfile(
            gender = preferences.enumValue(KEY_GENDER, fallback.gender),
            ageRange = preferences.enumValue(KEY_AGE_RANGE, fallback.ageRange),
            career = preferences.enumValue(KEY_CAREER, fallback.career),
            bedtime = preferences.getString(KEY_BEDTIME, fallback.bedtime) ?: fallback.bedtime,
            wakeTime = preferences.getString(KEY_WAKE_TIME, fallback.wakeTime) ?: fallback.wakeTime,
            sleepIssues = preferences.enumSet(KEY_SLEEP_ISSUES, fallback.sleepIssues),
            contentPreferences = preferences.enumSet(KEY_CONTENT_PREFERENCES, fallback.contentPreferences),
            companionStyle = preferences.enumValue(KEY_COMPANION_STYLE, fallback.companionStyle),
            voicePreference = preferences.enumValue(KEY_VOICE_PREFERENCE, fallback.voicePreference),
            companionPrompt = preferences.getString(KEY_COMPANION_PROMPT, fallback.companionPrompt)
                ?: fallback.companionPrompt
        )
    }

    fun saveProfile(profile: UserProfile) {
        preferences.edit()
            .putBoolean(KEY_HAS_PROFILE, true)
            .putString(KEY_GENDER, profile.gender.name)
            .putString(KEY_AGE_RANGE, profile.ageRange.name)
            .putString(KEY_CAREER, profile.career.name)
            .putString(KEY_BEDTIME, profile.bedtime)
            .putString(KEY_WAKE_TIME, profile.wakeTime)
            .putString(KEY_SLEEP_ISSUES, profile.sleepIssues.encodeNames())
            .putString(KEY_CONTENT_PREFERENCES, profile.contentPreferences.encodeNames())
            .putString(KEY_COMPANION_STYLE, profile.companionStyle.name)
            .putString(KEY_VOICE_PREFERENCE, profile.voicePreference.name)
            .putString(KEY_COMPANION_PROMPT, profile.companionPrompt)
            .apply()
    }

    private inline fun <reified T : Enum<T>> SharedPreferences.enumValue(key: String, fallback: T): T {
        val stored = getString(key, null) ?: return fallback
        return runCatching { enumValueOf<T>(stored) }.getOrDefault(fallback)
    }

    private inline fun <reified T : Enum<T>> SharedPreferences.enumSet(
        key: String,
        fallback: Set<T>
    ): Set<T> {
        val stored = getString(key, null) ?: return fallback
        val values = stored.split(SET_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull { name -> runCatching { enumValueOf<T>(name) }.getOrNull() }
            .toSet()

        return values.ifEmpty { fallback }
    }

    private fun <T : Enum<T>> Set<T>.encodeNames(): String {
        return joinToString(SET_SEPARATOR) { it.name }
    }

    private companion object {
        const val PREFERENCES_NAME = "floppy_profile"
        const val SET_SEPARATOR = ","
        const val KEY_USER_ID = "user_id"
        const val KEY_HAS_PROFILE = "has_profile"
        const val KEY_GENDER = "gender"
        const val KEY_AGE_RANGE = "age_range"
        const val KEY_CAREER = "career"
        const val KEY_BEDTIME = "bedtime"
        const val KEY_WAKE_TIME = "wake_time"
        const val KEY_SLEEP_ISSUES = "sleep_issues"
        const val KEY_CONTENT_PREFERENCES = "content_preferences"
        const val KEY_COMPANION_STYLE = "companion_style"
        const val KEY_VOICE_PREFERENCE = "voice_preference"
        const val KEY_COMPANION_PROMPT = "companion_prompt"
    }
}
