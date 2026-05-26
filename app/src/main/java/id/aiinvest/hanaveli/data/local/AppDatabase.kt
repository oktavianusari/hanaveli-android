package id.aiinvest.hanaveli.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import id.aiinvest.hanaveli.data.local.dao.MonitoredCurrencyDao
import id.aiinvest.hanaveli.data.local.dao.TransactionDao
import id.aiinvest.hanaveli.data.local.entity.MonitoredCurrency
import id.aiinvest.hanaveli.data.local.entity.Transaction

@Database(entities = [Transaction::class, MonitoredCurrency::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun monitoredCurrencyDao(): MonitoredCurrencyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "valas_monitor_database"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
