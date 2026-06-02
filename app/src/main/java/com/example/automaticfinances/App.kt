package com.example.automaticfinances

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.MerchantResolutionRepository
import com.example.automaticfinances.system.ListenerRebindWorker
import com.example.automaticfinances.system.ServiceManager
import com.example.automaticfinances.system.VoiceQuickActionNotifier
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var merchantResolutionRepository: MerchantResolutionRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // The database is provided by Hilt (see AppModule). No separate static instance is
        // created here anymore — keeping two Room instances open on the same file caused
        // independent connection pools / invalidation trackers.
        appScope.launch {
            categoryRepository.initializeDefaultCategories()
            // Seed the gateway→merchant mappings AFTER categories exist: the seed maps category
            // names to their ids, so the categories must already be persisted.
            merchantResolutionRepository.initializeDefaultResolutions()
        }
        // No foreground service to start: the NotificationListenerService is bound and kept
        // alive by the system once the user grants notification access. We still nudge the system
        // to (re)bind it now so detection goes live promptly after a cold start instead of waiting
        // for the next incoming notification.
        ServiceManager.requestListenerRebind(this)

        // Schedule the periodic rebind heartbeat (every 15 min — WorkManager's minimum period). KEEP
        // preserves the existing schedule across restarts so we don't reset the interval each launch.
        // This heals listeners that aggressive OEMs silently unbind while the app is idle.
        val rebindWork = PeriodicWorkRequestBuilder<ListenerRebindWorker>(
            15, TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ListenerRebindWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            rebindWork,
        )

        // Persistent quick action for voice entry (ongoing notification, no foreground service).
        // No-ops cleanly if POST_NOTIFICATIONS isn't granted yet; MainActivity re-posts it on
        // resume once the permission is granted.
        VoiceQuickActionNotifier.show(this)
    }
}
