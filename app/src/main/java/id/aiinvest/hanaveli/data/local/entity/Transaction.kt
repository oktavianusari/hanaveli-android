package id.aiinvest.hanaveli.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val currency: String, // e.g. "USD", "EUR"
    val type: String,     // "BUY" or "SELL"
    val rate: Double,     // Exchange rate in IDR
    val amount: Double,   // Amount of foreign currency
    val timestamp: Long   // Transaction time
)
