package id.aiinvest.hanaveli.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import id.aiinvest.hanaveli.data.local.dao.MonitoredCurrencyDao
import id.aiinvest.hanaveli.data.local.dao.StockDao
import id.aiinvest.hanaveli.data.local.dao.TransactionDao
import id.aiinvest.hanaveli.data.local.entity.MonitoredCurrency
import id.aiinvest.hanaveli.data.local.entity.MonitoredStock
import id.aiinvest.hanaveli.data.local.entity.SahamTransaction
import id.aiinvest.hanaveli.data.local.entity.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Transaction::class, MonitoredCurrency::class, MonitoredStock::class, SahamTransaction::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun monitoredCurrencyDao(): MonitoredCurrencyDao
    abstract fun stockDao(): StockDao
    abstract fun sahamTransactionDao(): id.aiinvest.hanaveli.data.local.dao.SahamTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `monitored_stocks` (`symbol` TEXT NOT NULL, `name` TEXT NOT NULL, `currency` TEXT NOT NULL, `price` REAL NOT NULL, `prevClose` REAL NOT NULL, `changePercent` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`symbol`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `saham_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `symbol` TEXT NOT NULL, `type` TEXT NOT NULL, `lot` INTEGER NOT NULL, `price` REAL NOT NULL, `timestamp` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE monitored_stocks ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "valas_monitor_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
