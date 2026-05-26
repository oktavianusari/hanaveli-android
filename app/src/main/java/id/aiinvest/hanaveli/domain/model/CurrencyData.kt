package id.aiinvest.hanaveli.domain.model

data class CurrencyData(
    val currencyCode: String,
    val displayOrder: Int,
    val currentRate: Double,
    val baseRateToday: Double,
    val balance: Double,
    val averageBuyPrice: Double,
    val totalCost: Double,
    val realizedGain: Double,
    val unrealizedGain: Double,
    val totalGain: Double
) {
    val dailyChangePercent: Double
        get() = if (baseRateToday > 0) ((currentRate - baseRateToday) / baseRateToday) * 100 else 0.0
}
