package id.aiinvest.hanaveli.ui.utils

fun getFlagEmojiForCurrency(currencyCode: String): String {
    return when (currencyCode.uppercase()) {
        "USD" -> "🇺🇸"
        "EUR" -> "🇪🇺"
        "GBP" -> "🇬🇧"
        "AUD" -> "🇦🇺"
        "CAD" -> "🇨🇦"
        "CHF" -> "🇨🇭"
        "CNY" -> "🇨🇳"
        "HKD" -> "🇭🇰"
        "JPY" -> "🇯🇵"
        "MYR" -> "🇲🇾"
        "NZD" -> "🇳🇿"
        "SAR" -> "🇸🇦"
        "SEK" -> "🇸🇪"
        "SGD" -> "🇸🇬"
        "THB" -> "🇹🇭"
        "RUB" -> "🇷🇺"
        "KWD" -> "🇰🇼"
        "EMAS" -> "🪙"
        else -> "🏳️"
    }
}
