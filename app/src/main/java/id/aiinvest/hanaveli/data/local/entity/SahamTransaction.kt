package id.aiinvest.hanaveli.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saham_transactions")
data class SahamTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,   // e.g. "BBCA.JK"
    val type: String,     // "BUY" atau "SELL"
    val lot: Int,         // Jumlah lot
    val price: Double,    // Harga eksekusi per lembar
    val timestamp: Long   // Waktu transaksi
)
