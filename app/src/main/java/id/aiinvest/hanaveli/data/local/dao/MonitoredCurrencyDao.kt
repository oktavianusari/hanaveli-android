package id.aiinvest.hanaveli.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import id.aiinvest.hanaveli.data.local.entity.MonitoredCurrency
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredCurrencyDao {
    @Query("SELECT * FROM monitored_currencies ORDER BY displayOrder ASC")
    fun getAllMonitoredCurrencies(): Flow<List<MonitoredCurrency>>

    @Query("SELECT * FROM monitored_currencies ORDER BY displayOrder ASC")
    fun getAllMonitoredCurrenciesSync(): List<MonitoredCurrency>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonitoredCurrency(currency: MonitoredCurrency)

    @Update
    suspend fun updateMonitoredCurrency(currency: MonitoredCurrency)

    @Update
    suspend fun updateMonitoredCurrencies(currencies: List<MonitoredCurrency>)

    @Delete
    suspend fun deleteMonitoredCurrency(currency: MonitoredCurrency)

    @Query("SELECT MAX(displayOrder) FROM monitored_currencies")
    suspend fun getMaxDisplayOrder(): Int?
}
