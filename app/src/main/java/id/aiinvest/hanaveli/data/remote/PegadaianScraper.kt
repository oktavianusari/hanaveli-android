package id.aiinvest.hanaveli.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.regex.Pattern
import id.aiinvest.hanaveli.data.remote.ScrapedRate
import id.aiinvest.hanaveli.data.remote.ScrapeResult

object PegadaianScraper {
    private const val URL = "https://www.indogold.id/detail-emas-batangan"

    suspend fun scrapeRates(): ScrapeResult {
        return withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, ScrapedRate>()
            var lastUpdatedStr = ""
            var buyPrice = 0.0
            var sellPrice = 0.0

            try {
                val doc = Jsoup.connect(URL).userAgent("Mozilla/5.0").get()
                
                val rawHtml = doc.html()
                
                // Cari harga 1.0 Gram Tahun 2026 dari JSON attribute
                val pricePattern = Pattern.compile("&quot;nama&quot;:&quot;LM Antam 99.99% 1.0 Gram&quot;.*?&quot;tahun&quot;:&quot;2026&quot;.*?&quot;harga_beli&quot;:&quot;([0-9,]+)&quot;.*?&quot;harga_jual&quot;:&quot;([0-9,]+)&quot;")
                val priceMatcher = pricePattern.matcher(rawHtml)
                if (priceMatcher.find()) {
                    val beliStr = priceMatcher.group(1).replace(",", "")
                    val jualStr = priceMatcher.group(2).replace(",", "")
                    sellPrice = beliStr.toDoubleOrNull() ?: 0.0 // Harga Beli user
                    buyPrice = jualStr.toDoubleOrNull() ?: 0.0 // Harga Jual user
                }
                val matcher = java.util.regex.Pattern.compile("Last Update\\s*:\\s*\\d{1,2}\\s+[a-zA-Z]+\\s+\\d{4}\\s+\\d{2}:\\d{2}", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(rawHtml)
                if (matcher.find()) {
                    lastUpdatedStr = matcher.group().trim()
                } else {
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
                    lastUpdatedStr = "Last Update : " + sdf.format(java.util.Date())
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (buyPrice == 0.0) {
                // Fallback jika gagal
                buyPrice = 2690000.0 // Harga Jual User
                sellPrice = 2911000.0 // Harga Beli User
                if (lastUpdatedStr.isEmpty()) {
                    val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
                    lastUpdatedStr = "Last Update : " + sdf.format(java.util.Date())
                }
            }

            val rate = ScrapedRate(
                currency = "EMAS",
                eRateBuy = sellPrice,
                eRateSell = buyPrice,
                bankNotesBuy = sellPrice,
                bankNotesSell = buyPrice,
                ttCounterBuy = sellPrice,
                ttCounterSell = buyPrice
            )
            result["EMAS"] = rate
            
            ScrapeResult(rates = result, lastUpdated = lastUpdatedStr)
        }
    }
}
