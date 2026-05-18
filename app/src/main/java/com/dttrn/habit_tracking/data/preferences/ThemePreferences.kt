package com.dttrn.habit_tracking.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme(val value: String) {
    LIGHT("LIGHT"),
    DARK("DARK"),
    SYSTEM("SYSTEM")
}

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(getSavedTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private fun getSavedTheme(): AppTheme {
        val saved = prefs.getString(KEY_THEME, AppTheme.SYSTEM.value) ?: AppTheme.SYSTEM.value
        return AppTheme.entries.firstOrNull { it.value == saved } ?: AppTheme.SYSTEM
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.value).apply()
        _theme.value = theme
    }

    companion object {
        private const val PREFS_NAME = "habit_journey_prefs"
        private const val KEY_THEME = "app_theme"
    }
}
