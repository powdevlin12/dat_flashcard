package com.dttrn.datfs.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quản lý tất cả user preferences qua DataStore.
 * Thay thế SharedPreferences cũ (ThemePreferences).
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<androidx.datastore.preferences.core.Preferences>
) {
    // ===== Keys =====
    private object Keys {
        val THEME = stringPreferencesKey("theme")                    // LIGHT / DARK / SYSTEM
        val FONT_SIZE = stringPreferencesKey("font_size")            // SMALL / MEDIUM / LARGE
        val DEFAULT_STUDY_MODE = stringPreferencesKey("default_study_mode")  // SWIPE / LEARN / etc
        val ANIMATION_ENABLED = booleanPreferencesKey("animation_enabled")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")   // 0-23
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute") // 0-59
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val EXPORT_FORMAT = stringPreferencesKey("export_format")    // JSON / EXCEL
        val SEARCH_HISTORY = stringPreferencesKey("search_history")  // JSON array of strings
    }

    // ===== Defaults =====
    companion object {
        const val DEFAULT_THEME = "SYSTEM"
        const val DEFAULT_FONT_SIZE = "MEDIUM"
        const val DEFAULT_STUDY_MODE = "SWIPE"
        const val DEFAULT_EXPORT_FORMAT = "EXCEL"
        const val DEFAULT_NOTIFICATION_HOUR = 20
        const val DEFAULT_NOTIFICATION_MINUTE = 0
    }

    // ===== Flows =====

    val theme: Flow<String> = dataStore.data
        .catchIOError()
        .map { it[Keys.THEME] ?: DEFAULT_THEME }

    val fontSize: Flow<String> = dataStore.data
        .catchIOError()
        .map { it[Keys.FONT_SIZE] ?: DEFAULT_FONT_SIZE }

    val defaultStudyMode: Flow<String> = dataStore.data
        .catchIOError()
        .map { it[Keys.DEFAULT_STUDY_MODE] ?: DEFAULT_STUDY_MODE }

    val animationEnabled: Flow<Boolean> = dataStore.data
        .catchIOError()
        .map { it[Keys.ANIMATION_ENABLED] ?: true }

    val notificationEnabled: Flow<Boolean> = dataStore.data
        .catchIOError()
        .map { it[Keys.NOTIFICATION_ENABLED] ?: false }

    val notificationHour: Flow<Int> = dataStore.data
        .catchIOError()
        .map { it[Keys.NOTIFICATION_HOUR] ?: DEFAULT_NOTIFICATION_HOUR }

    val notificationMinute: Flow<Int> = dataStore.data
        .catchIOError()
        .map { it[Keys.NOTIFICATION_MINUTE] ?: DEFAULT_NOTIFICATION_MINUTE }

    val onboardingDone: Flow<Boolean> = dataStore.data
        .catchIOError()
        .map { it[Keys.ONBOARDING_DONE] ?: false }

    val exportFormat: Flow<String> = dataStore.data
        .catchIOError()
        .map { it[Keys.EXPORT_FORMAT] ?: DEFAULT_EXPORT_FORMAT }

    val searchHistory: Flow<List<String>> = dataStore.data
        .catchIOError()
        .map { prefs ->
            prefs[Keys.SEARCH_HISTORY]
                ?.split("|||")
                ?.filter { it.isNotBlank() }
                ?.take(10)
                ?: emptyList()
        }

    // ===== Setters =====

    suspend fun setTheme(theme: String) = dataStore.edit { it[Keys.THEME] = theme }
    suspend fun setFontSize(size: String) = dataStore.edit { it[Keys.FONT_SIZE] = size }
    suspend fun setDefaultStudyMode(mode: String) = dataStore.edit { it[Keys.DEFAULT_STUDY_MODE] = mode }
    suspend fun setAnimationEnabled(enabled: Boolean) = dataStore.edit { it[Keys.ANIMATION_ENABLED] = enabled }
    suspend fun setNotificationEnabled(enabled: Boolean) = dataStore.edit { it[Keys.NOTIFICATION_ENABLED] = enabled }
    suspend fun setNotificationTime(hour: Int, minute: Int) = dataStore.edit {
        it[Keys.NOTIFICATION_HOUR] = hour
        it[Keys.NOTIFICATION_MINUTE] = minute
    }
    suspend fun setOnboardingDone() = dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    suspend fun setExportFormat(format: String) = dataStore.edit { it[Keys.EXPORT_FORMAT] = format }

    suspend fun addSearchQuery(query: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.SEARCH_HISTORY]
                ?.split("|||")
                ?.filter { it.isNotBlank() }
                ?.toMutableList()
                ?: mutableListOf()
            current.remove(query)           // Xóa nếu đã tồn tại (để re-insert lên đầu)
            current.add(0, query)
            prefs[Keys.SEARCH_HISTORY] = current.take(10).joinToString("|||")
        }
    }

    suspend fun clearSearchHistory() {
        dataStore.edit { it.remove(Keys.SEARCH_HISTORY) }
    }
}

// Extension helper để bắt IOException khi đọc DataStore
private fun Flow<androidx.datastore.preferences.core.Preferences>.catchIOError(): Flow<androidx.datastore.preferences.core.Preferences> =
    catch { e ->
        if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
        else throw e
    }
