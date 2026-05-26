package id.aiinvest.hanaveli.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_currencies")
data class MonitoredCurrency(
    @PrimaryKey val currency: String, // e.g. "USD"
    val displayOrder: Int,
    val baseRateToday: Double = 0.0,
    val lastRate: Double = 0.0
)
