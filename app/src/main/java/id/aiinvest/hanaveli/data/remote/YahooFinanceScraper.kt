package id.aiinvest.hanaveli.data.remote

import id.aiinvest.hanaveli.data.local.entity.MonitoredStock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object YahooFinanceScraper {
    
    suspend fun scrapeStock(symbol: String): MonitoredStock? = withContext(Dispatchers.IO) {
        try {
            val upperSymbol = symbol.uppercase()
            val encodedSymbol = URLEncoder.encode(upperSymbol, "UTF-8")
            // Yahoo Finance v8 chart API - no key needed for simple public data
            val urlString = "https://query1.finance.yahoo.com/v8/finance/chart/$encodedSymbol?interval=1d&range=1d"
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = InputStreamReader(connection.inputStream).use { it.readText() }
                val json = JSONObject(jsonString)
                val chart = json.optJSONObject("chart")
                val resultArray = chart?.optJSONArray("result")
                
                if (resultArray != null && resultArray.length() > 0) {
                    val resultObj = resultArray.getJSONObject(0)
                    val meta = resultObj.optJSONObject("meta")
                    
                    if (meta != null) {
                        val price = meta.optDouble("regularMarketPrice", 0.0)
                        val prevClose = meta.optDouble("chartPreviousClose", price)
                        val currency = meta.optString("currency", "USD")
                        val name = meta.optString("longName", meta.optString("shortName", upperSymbol))
                        
                        val changePercent = if (prevClose > 0) ((price - prevClose) / prevClose) * 100 else 0.0
                        
                        return@withContext MonitoredStock(
                            symbol = upperSymbol,
                            name = name,
                            currency = currency,
                            price = price,
                            prevClose = prevClose,
                            changePercent = changePercent,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    // Function to bulk fetch multiple symbols, sequential for simplicity, but can be concurrent
    suspend fun scrapeStocks(symbols: List<String>): List<MonitoredStock> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MonitoredStock>()
        for (symbol in symbols) {
            val data = scrapeStock(symbol)
            if (data != null) {
                results.add(data)
            }
        }
        return@withContext results
    }
}
