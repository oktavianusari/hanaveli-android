package id.aiinvest.hanaveli.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.aiinvest.hanaveli.domain.model.CurrencyData
import id.aiinvest.hanaveli.ui.viewmodel.MonitorViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotalScreen(monitorViewModel: MonitorViewModel) {
    val currencies by monitorViewModel.monitoredCurrencies.collectAsState()
    val totalCurrencies = currencies.filter { it.balance > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Total Portofolio") },
                windowInsets = WindowInsets(0.dp)
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        if (totalCurrencies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Belum ada aset valas yang terbeli.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                items(totalCurrencies) { data ->
                    TotalCurrencyItem(data)
                }
            }
        }
    }
}

@Composable
fun TotalCurrencyItem(data: CurrencyData) {
    val formatIdr = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val formatForeign = NumberFormat.getNumberInstance(Locale("en", "US"))
    formatForeign.minimumFractionDigits = 2
    formatForeign.maximumFractionDigits = 2

    val rupiahValue = data.balance * data.currentRate
    val flag = getFlagEmoji(data.currencyCode)

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(text = "$flag ${data.currencyCode}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = formatForeign.format(data.balance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Estimasi Nilai Rupiah:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(formatIdr.format(rupiahValue), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun getFlagEmoji(currencyCode: String): String {
    return when (currencyCode.uppercase()) {
        "USD" -> "🇺🇸"
        "SGD" -> "🇸🇬"
        "EUR" -> "🇪🇺"
        "AUD" -> "🇦🇺"
        "DKK" -> "🇩🇰"
        "SEK" -> "🇸🇪"
        "CAD" -> "🇨🇦"
        "CHF" -> "🇨🇭"
        "NZD" -> "🇳🇿"
        "GBP" -> "🇬🇧"
        "HKD" -> "🇭🇰"
        "JPY" -> "🇯🇵"
        "SAR" -> "🇸🇦"
        "CNY" -> "🇨🇳"
        "MYR" -> "🇲🇾"
        "THB" -> "🇹🇭"
        "KRW" -> "🇰🇷"
        "RUB" -> "🇷🇺"
        "KWD" -> "🇰🇼"
        "EMAS" -> "🪙"
        else -> "🏳️"
    }
}
