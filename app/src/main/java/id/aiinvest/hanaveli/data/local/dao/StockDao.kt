package id.aiinvest.hanaveli.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import id.aiinvest.hanaveli.data.local.entity.MonitoredStock
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM monitored_stocks ORDER BY displayOrder ASC")
    fun getAllStocks(): Flow<List<MonitoredStock>>

    @Query("SELECT * FROM monitored_stocks ORDER BY displayOrder ASC")
    suspend fun getAllStocksSync(): List<MonitoredStock>

    @Query("SELECT MAX(displayOrder) FROM monitored_stocks")
    suspend fun getMaxDisplayOrder(): Int?

    @Update
    suspend fun updateStocks(stocks: List<MonitoredStock>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: MonitoredStock)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<MonitoredStock>)

    @Update
    suspend fun updateStock(stock: MonitoredStock)

    @Delete
    suspend fun deleteStock(stock: MonitoredStock)

    @Query("DELETE FROM monitored_stocks WHERE symbol = :symbol")
    suspend fun deleteStockBySymbol(symbol: String)
}
