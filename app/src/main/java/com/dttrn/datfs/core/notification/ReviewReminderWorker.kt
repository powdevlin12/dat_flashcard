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
import com.dttrn.datfs.core.data.local.dao.FlashcardDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

/**
 * WorkManager worker: kiểm tra số thẻ cần ôn hôm nay và gửi notification.
 * Được schedule daily theo giờ user chọn trong Settings.
 */
@HiltWorker
class ReviewReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val flashcardDao: FlashcardDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Lấy cuối ngày hôm nay
            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val dueCount = flashcardDao.getDueCardsCount(todayEnd)

            if (dueCount > 0) {
                sendNotification(dueCount)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendNotification(dueCount: Int) {
        // Kiểm tra permission trên API 33+
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
            NOTIFICATION_ID_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.CHANNEL_DAILY_REVIEW)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📚 Đã đến giờ ôn bài!")
            .setContentText("Bạn có $dueCount thẻ cần ôn hôm nay. Bắt đầu ngay nào!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID_DAILY, notification)
    }

    companion object {
        const val WORK_NAME = "review_reminder"
        private const val NOTIFICATION_ID_DAILY = 1001
    }
}
