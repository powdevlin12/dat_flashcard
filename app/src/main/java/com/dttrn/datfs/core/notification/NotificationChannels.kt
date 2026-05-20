package com.dttrn.datfs.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Tạo các Notification Channels cho FlashMind.
 * Channels phải được tạo trước khi gửi bất kỳ notification nào (API 26+).
 */
object NotificationChannels {

    const val CHANNEL_DAILY_REVIEW = "flashmind_daily_review"
    const val CHANNEL_OVERDUE = "flashmind_overdue"
    const val CHANNEL_STREAK = "flashmind_streak"

    /**
     * Tạo tất cả notification channels. Gọi trong Application.onCreate().
     * Nếu channel đã tồn tại, hệ thống sẽ bỏ qua.
     */
    fun createAll(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                CHANNEL_DAILY_REVIEW,
                "Nhắc ôn hàng ngày",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo nhắc nhở ôn bài hàng ngày theo lịch đặt"
            },

            NotificationChannel(
                CHANNEL_OVERDUE,
                "Thẻ quá hạn",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cảnh báo khi có nhiều thẻ quá hạn chưa ôn"
            },

            NotificationChannel(
                CHANNEL_STREAK,
                "Streak nhắc nhở",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nhắc nhở duy trì streak học tập lúc 23:00"
            },
        )

        manager.createNotificationChannels(channels)
    }
}
