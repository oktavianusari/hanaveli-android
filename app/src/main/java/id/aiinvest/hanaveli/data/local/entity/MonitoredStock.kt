package id.aiinvest.hanaveli.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_stocks")
data class MonitoredStock(
    @PrimaryKey
    val symbol: String, // e.g. BBCA.JK
    val name: String,
    val currency: String,
    val price: Double,
    val prevClose: Double,
    val changePercent: Double,
    val lastUpdated: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0
)
