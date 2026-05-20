package com.dttrn.datfs

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dttrn.datfs.core.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // Tạo notification channels khi app khởi động
        NotificationChannels.createAll(this)
    }

    /**
     * Cung cấp WorkManager Configuration với HiltWorkerFactory
     * để các @HiltWorker có thể inject dependencies.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
