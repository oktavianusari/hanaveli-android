package id.aiinvest.hanaveli.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import id.aiinvest.hanaveli.data.local.entity.SahamTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SahamTransactionDao {
    @Query("SELECT * FROM saham_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<SahamTransaction>>

    @Query("SELECT * FROM saham_transactions ORDER BY timestamp ASC")
    suspend fun getAllTransactionsSync(): List<SahamTransaction>

    @Query("SELECT * FROM saham_transactions WHERE symbol = :symbol ORDER BY timestamp ASC")
    fun getTransactionsBySymbol(symbol: String): Flow<List<SahamTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SahamTransaction)

    @Update
    suspend fun updateTransaction(transaction: SahamTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: SahamTransaction)
}
