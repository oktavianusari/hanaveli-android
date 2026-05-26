package id.aiinvest.hanaveli

import android.app.Application
import android.content.Context
import id.aiinvest.hanaveli.worker.SyncWorker

class ValasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val prefs = getSharedPreferences("valas_settings", Context.MODE_PRIVATE)
        // Default to 60 minutes (1 hour) as requested by user
        val interval = prefs.getInt("sync_interval", 60)
        
        SyncWorker.enqueue(this, interval.toLong())
    }
}
