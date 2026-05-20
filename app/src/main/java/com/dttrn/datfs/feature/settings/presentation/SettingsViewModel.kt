package com.dttrn.datfs.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.data.datastore.SettingsDataStore
import com.dttrn.datfs.core.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho SettingsScreen.
 * Đọc tất cả preferences từ DataStore, expose qua StateFlow,
 * cung cấp hàm update cho mỗi setting.
 * Quản lý scheduling notification workers khi toggle.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val notificationScheduler: NotificationScheduler,
) : ViewModel() {

    // ===== State Flows =====

    val theme: StateFlow<String> = settingsDataStore.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_THEME)

    val fontSize: StateFlow<String> = settingsDataStore.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_FONT_SIZE)

    val defaultStudyMode: StateFlow<String> = settingsDataStore.defaultStudyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_STUDY_MODE)

    val animationEnabled: StateFlow<Boolean> = settingsDataStore.animationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationEnabled: StateFlow<Boolean> = settingsDataStore.notificationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notificationHour: StateFlow<Int> = settingsDataStore.notificationHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_NOTIFICATION_HOUR)

    val notificationMinute: StateFlow<Int> = settingsDataStore.notificationMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_NOTIFICATION_MINUTE)

    val exportFormat: StateFlow<String> = settingsDataStore.exportFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.DEFAULT_EXPORT_FORMAT)

    // ===== Actions =====

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsDataStore.setTheme(theme) }
    }

    fun setFontSize(size: String) {
        viewModelScope.launch { settingsDataStore.setFontSize(size) }
    }

    fun setDefaultStudyMode(mode: String) {
        viewModelScope.launch { settingsDataStore.setDefaultStudyMode(mode) }
    }

    fun setAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAnimationEnabled(enabled) }
    }

    /**
     * Bật/tắt notification. Khi bật → schedule workers,
     * khi tắt → cancel workers.
     */
    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setNotificationEnabled(enabled)
            if (enabled) {
                val hour = notificationHour.value
                val minute = notificationMinute.value
                notificationScheduler.scheduleReviewReminder(hour, minute)
                notificationScheduler.scheduleStreakReminder()
            } else {
                notificationScheduler.cancelAll()
            }
        }
    }

    /**
     * Cập nhật giờ nhắc nhở. Re-schedule review reminder worker
     * với thời gian mới.
     */
    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.setNotificationTime(hour, minute)
            if (notificationEnabled.value) {
                notificationScheduler.scheduleReviewReminder(hour, minute)
            }
        }
    }

    fun setExportFormat(format: String) {
        viewModelScope.launch { settingsDataStore.setExportFormat(format) }
    }
}
