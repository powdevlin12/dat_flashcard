package com.dttrn.datfs.core.notification

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quản lý việc lên lịch / hủy các periodic WorkManager workers
 * cho notification. Settings thay đổi sẽ gọi lại schedule/cancel.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /**
     * Lên lịch nhắc nhở ôn bài hàng ngày vào giờ [hour]:[minute].
     * Sử dụng PeriodicWorkRequest với khoảng cách 24h.
     * Nếu đã có worker cũ, sẽ thay thế.
     */
    fun scheduleReviewReminder(hour: Int, minute: Int) {
        val initialDelay = calculateInitialDelay(hour, minute)

        val request = PeriodicWorkRequestBuilder<ReviewReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag(TAG_REVIEW_REMINDER)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ReviewReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * Lên lịch streak reminder lúc 23:00 mỗi ngày.
     */
    fun scheduleStreakReminder() {
        val initialDelay = calculateInitialDelay(23, 0)

        val request = PeriodicWorkRequestBuilder<StreakReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .addTag(TAG_STREAK_REMINDER)
            .build()

        workManager.enqueueUniquePeriodicWork(
            StreakReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * Hủy tất cả notification workers.
     */
    fun cancelAll() {
        workManager.cancelUniqueWork(ReviewReminderWorker.WORK_NAME)
        workManager.cancelUniqueWork(StreakReminderWorker.WORK_NAME)
    }

    /**
     * Hủy chỉ review reminder.
     */
    fun cancelReviewReminder() {
        workManager.cancelUniqueWork(ReviewReminderWorker.WORK_NAME)
    }

    /**
     * Tính delay từ hiện tại đến giờ target.
     * Nếu giờ target đã qua trong ngày hôm nay → schedule cho ngày mai.
     */
    private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Nếu giờ target đã qua hôm nay, schedule cho ngày mai
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        private const val TAG_REVIEW_REMINDER = "tag_review_reminder"
        private const val TAG_STREAK_REMINDER = "tag_streak_reminder"
    }
}
