package com.dttrn.datfs.core.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dttrn.datfs.MainActivity
import com.dttrn.datfs.R
import com.dttrn.datfs.core.data.local.dao.StudyStatisticsDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WorkManager worker: kiểm tra streak lúc 23:00 hàng ngày.
 * Nếu hôm nay chưa học gì, gửi notification nhắc nhở
 * để user không mất streak.
 */
@HiltWorker
class StreakReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val studyStatisticsDao: StudyStatisticsDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val todayStats = studyStatisticsDao.getStatsByDateOnce(todayStr)

            // Nếu hôm nay chưa học gì → nhắc nhở giữ streak
            if (todayStats == null || todayStats.cardsStudied == 0) {
                // Lấy streak hiện tại
                val currentStreak = studyStatisticsDao.getCurrentStreakOnce()
                if (currentStreak > 0) {
                    sendStreakReminder(currentStreak)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendStreakReminder(streakDays: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID_STREAK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.CHANNEL_STREAK)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔥 Đừng để mất streak!")
            .setContentText("Học ít nhất 1 thẻ để duy trì streak $streakDays ngày! Còn ít phút nữa thôi.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID_STREAK, notification)
    }

    companion object {
        const val WORK_NAME = "streak_reminder"
        private const val NOTIFICATION_ID_STREAK = 1002
    }
}
