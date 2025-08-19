package com.example.automaticfinances

import android.app.Application
import com.example.automaticfinances.data.db.AppDatabase
import com.example.automaticfinances.data.repo.CategoryRepository
import com.example.automaticfinances.system.ServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.init(this)
        
        // Inicializar categorías por defecto en background thread
        CoroutineScope(Dispatchers.IO).launch {
            CategoryRepository().initializeDefaultCategories()
        }
        
        // Start persistent service
        ServiceManager.startPersistentService(this)
    }
}