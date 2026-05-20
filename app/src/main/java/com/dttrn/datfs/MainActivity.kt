package com.dttrn.datfs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.dttrn.datfs.core.data.datastore.SettingsDataStore
import com.dttrn.datfs.core.notification.NotificationScheduler
import com.dttrn.datfs.navigation.MainScaffold
import com.dttrn.datfs.ui.theme.FlashMindTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule notifications nếu đã bật
        scheduleNotificationsIfNeeded()

        setContent {
            val theme by settingsDataStore.theme.collectAsState(initial = SettingsDataStore.DEFAULT_THEME)
            val isDark = when (theme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            FlashMindTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScaffold(settingsDataStore = settingsDataStore)
                }
            }
        }
    }

    /**
     * Khởi tạo notification schedule nếu user đã bật notification.
     * Gọi mỗi lần mở app để đảm bảo workers luôn được enqueue.
     */
    private fun scheduleNotificationsIfNeeded() {
        lifecycleScope.launch {
            val notifEnabled = settingsDataStore.notificationEnabled.first()
            if (notifEnabled) {
                val hour = settingsDataStore.notificationHour.first()
                val minute = settingsDataStore.notificationMinute.first()
                notificationScheduler.scheduleReviewReminder(hour, minute)
                notificationScheduler.scheduleStreakReminder()
            }
        }
    }
}
