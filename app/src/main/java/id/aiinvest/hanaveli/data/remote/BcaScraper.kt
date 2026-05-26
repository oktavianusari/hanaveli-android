package id.aiinvest.hanaveli.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.regex.Pattern

data class ScrapedRate(
    val currency: String,
    val eRateBuy: Double,
    val eRateSell: Double,
    val ttCounterBuy: Double,
    val ttCounterSell: Double,
    val bankNotesBuy: Double,
    val bankNotesSell: Double
)

data class ScrapeResult(
    val rates: Map<String, ScrapedRate>,
    val lastUpdated: String
)

object BcaScraper {
    private const val URL = "https://www.bca.co.id/id/informasi/kurs"

    suspend fun scrapeRates(): ScrapeResult = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, ScrapedRate>()
        var lastUpdatedStr = ""
        try {
            val doc = Jsoup.connect(URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get()

            // Extract last updated time using Regex
            val text = doc.text()
            val matcher = Pattern.compile("Terakhir diperbarui pada .*? WIB").matcher(text)
            if (matcher.find()) {
                lastUpdatedStr = matcher.group()
            }

            // Find all currency dropdown options in the first calculator or table
            val elements = doc.select("a.a-dropdown-currency1")
            for (element in elements) {
                val currency = element.attr("data-text").trim()
                val buyRates = element.attr("data-value-buy").split("-")
                val sellRates = element.attr("data-value-sell").split("-")

                if (currency.isNotEmpty() && buyRates.size == 3 && sellRates.size == 3) {
                    try {
                        val rate = ScrapedRate(
                            currency = currency,
                            eRateBuy = buyRates[0].toDoubleOrNull() ?: 0.0,
                            ttCounterBuy = buyRates[1].toDoubleOrNull() ?: 0.0,
                            bankNotesBuy = buyRates[2].toDoubleOrNull() ?: 0.0,
                            eRateSell = sellRates[0].toDoubleOrNull() ?: 0.0,
                            ttCounterSell = sellRates[1].toDoubleOrNull() ?: 0.0,
                            bankNotesSell = sellRates[2].toDoubleOrNull() ?: 0.0
                        )
                        // BCA duplicates these elements for different dropdowns on the page, just keep the first one
                        if (!result.containsKey(currency)) {
                            result[currency] = rate
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors for individual rows
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ScrapeResult(result, lastUpdatedStr)
    }
}
