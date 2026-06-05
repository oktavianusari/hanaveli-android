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
import androidx.compose.ui.draw.shadow
import id.aiinvest.hanaveli.domain.model.CurrencyData
import id.aiinvest.hanaveli.ui.viewmodel.MonitorViewModel
import id.aiinvest.hanaveli.ui.viewmodel.SettingsViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotalScreen(monitorViewModel: MonitorViewModel, settingsViewModel: SettingsViewModel) {
    val isSensitiveDataVisible by settingsViewModel.isSensitiveDataVisible.collectAsState()
    val currencies by monitorViewModel.monitoredCurrencies.collectAsState()
    val totalCurrencies = currencies.filter { it.balance > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Total Valas") },
                windowInsets = WindowInsets(0.dp),
                actions = {
                    IconButton(onClick = { settingsViewModel.toggleSensitiveDataVisibility() }) {
                        Icon(if (isSensitiveDataVisible) Icons.Filled.Clear else Icons.Filled.Lock, contentDescription = "Toggle Visibility")
                    }
                }
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
                item {
                    val totalPortfolioCost = totalCurrencies.sumOf { it.totalCost }
                    val totalPortfolioValue = totalCurrencies.sumOf { it.balance * it.currentRate }
                    val totalGainLoss = totalPortfolioValue - totalPortfolioCost
                    val gainPercent = if (totalPortfolioCost > 0) (totalGainLoss / totalPortfolioCost) * 100 else 0.0
                    val formatIdr = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
                    
                    val sign = if (totalGainLoss >= 0) "+" else "-"
                    val gainColor = if (totalGainLoss >= 0) androidx.compose.ui.graphics.Color(0xFF388E3C) else androidx.compose.ui.graphics.Color(0xFFD32F2F)
                    val absGain = Math.abs(totalGainLoss)
                    val absPercent = Math.abs(gainPercent)
                    val gainText = if (isSensitiveDataVisible) {
                        "$sign Rp ${String.format("%,.0f", absGain).replace(',', '.')} ($sign${String.format("%.2f", absPercent).replace('.', ',')}%)"
                    } else {
                        "Rp ---- (----%)"
                    }
                    val totalValueText = if (isSensitiveDataVisible) formatIdr.format(totalPortfolioValue) else "Rp ----"

                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).shadow(4.dp, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text("Estimasi Nilai Total Portofolio", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = totalValueText, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Estimasi Persentase Gain atau Loss", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = gainText, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = if (!isSensitiveDataVisible) androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant else gainColor)
                        }
                    }
                }
                items(totalCurrencies) { data ->
                    TotalCurrencyItem(data, isSensitiveDataVisible)
                }
            }
        }
    }
}

@Composable
fun TotalCurrencyItem(data: CurrencyData, isSensitiveDataVisible: Boolean) {
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
                Text(text = if (isSensitiveDataVisible) formatForeign.format(data.balance) else "----", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Estimasi Nilai Rupiah:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(if (isSensitiveDataVisible) formatIdr.format(rupiahValue) else "Rp ----", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
