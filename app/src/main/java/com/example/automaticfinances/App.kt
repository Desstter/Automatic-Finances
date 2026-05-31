package com.example.automaticfinances

import android.app.Application
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.data.repo.MerchantResolutionRepository
import com.example.automaticfinances.system.ServiceManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
        ServiceManager.startPersistentService(this)
    }
}
