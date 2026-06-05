package id.aiinvest.hanaveli.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import id.aiinvest.hanaveli.R
import id.aiinvest.hanaveli.ui.viewmodel.SettingsViewModel
import id.aiinvest.hanaveli.ui.viewmodel.TransactionViewModel
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, transactionViewModel: TransactionViewModel, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val rateType by viewModel.rateType.collectAsState()
    val rateDirection by viewModel.rateDirection.collectAsState()
    val emasRateDirection by viewModel.emasRateDirection.collectAsState()
    val syncInterval by viewModel.syncInterval.collectAsState()
    val themePreference by viewModel.themePreference.collectAsState()
    val language by viewModel.language.collectAsState()
    
    val overrideWidgetColor by viewModel.overrideWidgetColor.collectAsState()
    val widgetBgHex by viewModel.widgetBgHex.collectAsState()
    val widgetOpacity by viewModel.widgetOpacity.collectAsState()
    
    val lightPrimary by viewModel.lightPrimaryHex.collectAsState()
    val lightSecondary by viewModel.lightSecondaryHex.collectAsState()
    val lightText by viewModel.lightTextHex.collectAsState()
    
    val darkPrimary by viewModel.darkPrimaryHex.collectAsState()
    val darkSecondary by viewModel.darkSecondaryHex.collectAsState()
    val darkText by viewModel.darkTextHex.collectAsState()

    var langExpanded by remember { mutableStateOf(false) }
    var rateExpanded by remember { mutableStateOf(false) }
    var directionExpanded by remember { mutableStateOf(false) }
    var emasDirectionExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var intervalExpanded by remember { mutableStateOf(false) }

    val rateOptions = listOf("e-rate", "bank notes", "tt counter")
    val directionOptions = listOf("jual" to stringResource(R.string.rate_sell), "beli" to stringResource(R.string.rate_buy))
    val themeOptions = listOf("system" to stringResource(R.string.theme_system), "light" to stringResource(R.string.theme_light), "dark" to stringResource(R.string.theme_dark))
    val intervalOptions = listOf(1440 to stringResource(R.string.sync_1x_sehari), 720 to stringResource(R.string.sync_2x_sehari), 360 to stringResource(R.string.sync_4x_sehari), 180 to stringResource(R.string.sync_8x_sehari), 60 to stringResource(R.string.sync_24x_sehari))
    val langOptions = listOf("system" to stringResource(R.string.lang_system), "in" to stringResource(R.string.indonesian), "en" to stringResource(R.string.english))

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            transactionViewModel.exportTransactions(uri, context)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            transactionViewModel.importTransactions(uri, context)
        }
    }

    fun formatRateType(rt: String): String {
        return when(rt.lowercase()) {
            "e-rate" -> "e-Rate"
            "tt counter" -> "TT Counter"
            "bank notes" -> "Bank Notes"
            else -> rt
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Pencadangan Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { 
                        val dateFormat = java.text.SimpleDateFormat("ddMMyyyy_HHmma", java.util.Locale.US)
                        val fileName = "vm_${dateFormat.format(java.util.Date())}.json".lowercase()
                        exportLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ekspor Data")
                }
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Impor Data")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Language Dropdown
            ExposedDropdownMenuBox(
                expanded = langExpanded,
                onExpandedChange = { langExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                val currentLangName = langOptions.find { it.first == language }?.second ?: stringResource(R.string.indonesian)
                OutlinedTextField(
                    value = currentLangName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.app_language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                    langOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option.second) }, onClick = { 
                            viewModel.updateLanguage(option.first)
                            langExpanded = false 
                            Toast.makeText(context, "Pengaturan bahasa tersimpan", Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            }

        // Theme Dropdown
        ExposedDropdownMenuBox(
            expanded = themeExpanded,
            onExpandedChange = { themeExpanded = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            val currentThemeName = themeOptions.find { it.first == themePreference }?.second ?: stringResource(R.string.theme_system)
            OutlinedTextField(
                value = currentThemeName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.app_theme)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                themeOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option.second) }, onClick = { 
                        viewModel.updateTheme(option.first)
                        themeExpanded = false 
                        Toast.makeText(context, "Pengaturan tema tersimpan", Toast.LENGTH_SHORT).show()
                        
                        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                        val intent1 = android.content.Intent(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java).apply {
                            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, id.aiinvest.hanaveli.widget.ValasWidgetProvider::class.java))
                            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
                        context.sendBroadcast(intent1)

                        val intent2 = android.content.Intent(context, id.aiinvest.hanaveli.widget.BigValasWidgetProvider::class.java).apply {
                            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            val ids = appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, id.aiinvest.hanaveli.widget.BigValasWidgetProvider::class.java))
                            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                        }
                        context.sendBroadcast(intent2)
                    })
                }
            }
        }

        var tempOverride by remember(overrideWidgetColor) { mutableStateOf(overrideWidgetColor) }
        var tempWidgetBgHex by remember(widgetBgHex) { mutableStateOf(widgetBgHex) }
        var tempWidgetOpacity by remember(widgetOpacity) { mutableStateOf(widgetOpacity) }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Kustomisasi Warna Widget",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("Override Widget Color", modifier = Modifier.weight(1f))
            Switch(checked = tempOverride, onCheckedChange = { tempOverride = it })
        }
        
        if (tempOverride) {
            OutlinedTextField(
                value = tempWidgetBgHex, 
                onValueChange = { tempWidgetBgHex = it }, 
                label = { Text("Widget Background Color (Hex)") }, 
                placeholder = { Text("#000000") }, 
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            
            Text("Widget Opacity: ${(tempWidgetOpacity * 100).toInt()}%", modifier = Modifier.padding(bottom = 4.dp))
            Slider(
                value = tempWidgetOpacity,
                onValueChange = { tempWidgetOpacity = it },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            
            val widgetPresets = listOf("#000000", "#FFFFFF", "#151517", "#EFEFF4", "#1E88E5", "#43A047")
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                widgetPresets.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex)), shape = androidx.compose.foundation.shape.CircleShape)
                            .clickable { tempWidgetBgHex = hex }
                    )
                }
            }
        }
        
        Text(
            text = "Catatan: Jika Override OFF, widget mengikuti pengaturan tema (Light/Dark/System). Jika ON, widget menggunakan warna dan transparansi kustom di atas.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Button(onClick = { 
            viewModel.updateWidgetSettings(tempOverride, tempWidgetBgHex, tempWidgetOpacity)
            Toast.makeText(context, "Pengaturan Widget Tersimpan", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text("Simpan Pengaturan Widget")
        }

        // Rate Type Dropdown
        ExposedDropdownMenuBox(
            expanded = rateExpanded,
            onExpandedChange = { rateExpanded = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            OutlinedTextField(
                value = formatRateType(rateType),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.bca_rate_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rateExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = rateExpanded, onDismissRequest = { rateExpanded = false }) {
                rateOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(formatRateType(option)) }, onClick = { 
                        viewModel.updateRateType(option)
                        rateExpanded = false 
                        Toast.makeText(context, "Tipe kurs tersimpan", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }

        val rateTypeDesc = when (rateType.lowercase()) {
            "e-rate" -> stringResource(R.string.rate_desc_erate)
            "tt counter" -> stringResource(R.string.rate_desc_tt)
            else -> stringResource(R.string.rate_desc_banknotes)
        }
        Text(
            text = rateTypeDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Rate Direction Dropdown
        ExposedDropdownMenuBox(
            expanded = directionExpanded,
            onExpandedChange = { directionExpanded = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            val currentDirectionName = directionOptions.find { it.first == rateDirection }?.second ?: stringResource(R.string.rate_sell)
            OutlinedTextField(
                value = currentDirectionName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.rate_direction)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = directionExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = directionExpanded, onDismissRequest = { directionExpanded = false }) {
                directionOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option.second) }, onClick = { 
                        viewModel.updateRateDirection(option.first)
                        directionExpanded = false 
                        Toast.makeText(context, "Posisi kurs tersimpan", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }

        // Emas Rate Direction Dropdown
        ExposedDropdownMenuBox(
            expanded = emasDirectionExpanded,
            onExpandedChange = { emasDirectionExpanded = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            val currentEmasDirectionName = directionOptions.find { it.first == emasRateDirection }?.second ?: stringResource(R.string.rate_sell)
            OutlinedTextField(
                value = currentEmasDirectionName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.emas_rate_direction)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = emasDirectionExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = emasDirectionExpanded, onDismissRequest = { emasDirectionExpanded = false }) {
                directionOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option.second) }, onClick = { 
                        viewModel.updateEmasRateDirection(option.first)
                        emasDirectionExpanded = false 
                        Toast.makeText(context, "Patokan harga emas tersimpan", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }

        // Interval Dropdown
        ExposedDropdownMenuBox(
            expanded = intervalExpanded,
            onExpandedChange = { intervalExpanded = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            val currentIntervalName = intervalOptions.find { it.first == syncInterval }?.second ?: stringResource(R.string.sync_24x_sehari)
            OutlinedTextField(
                value = currentIntervalName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.sync_interval)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = intervalExpanded, onDismissRequest = { intervalExpanded = false }) {
                intervalOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option.second) }, onClick = { 
                        viewModel.updateSyncInterval(option.first)
                        intervalExpanded = false 
                        Toast.makeText(context, "Interval update tersimpan", Toast.LENGTH_SHORT).show()
                    })
                }
            }
        }

        // Fitur Keamanan Widget
        val enableWidgetShowHide by viewModel.enableWidgetShowHide.collectAsState()
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Fitur Keamanan Widget (Mata)", style = MaterialTheme.typography.bodyLarge)
                Text("Sembunyikan/Tampilkan Nominal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enableWidgetShowHide, onCheckedChange = { viewModel.updateEnableWidgetShowHide(it) })
        }

        // Auto Rate BCA
        val autoRateBca by viewModel.autoRateBca.collectAsState()
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto Rate BCA", style = MaterialTheme.typography.bodyLarge)
                Text("Jam 09:00 - 15:59 gunakan Rate Beli", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = autoRateBca, onCheckedChange = { viewModel.updateAutoRateBca(it) })
        }

        Text(
            text = stringResource(R.string.color_customization),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        val useDynamicColor by viewModel.useDynamicColor.collectAsState()
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text("Gunakan Warna Sistem (Material You)", modifier = Modifier.weight(1f))
            Switch(checked = useDynamicColor, onCheckedChange = { viewModel.updateUseDynamicColor(it) })
        }

        if (!useDynamicColor) {
            Text("Pilih Template Tema:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
            val templates = listOf(
                Pair("#386A20", "#9CD67D"),
                Pair("#0061A4", "#9ECAFF"),
                Pair("#B3261E", "#F2B8B5"),
                Pair("#6750A4", "#D0BCFF"),
                Pair("#8C5000", "#FFB776"),
                Pair("#9A25AE", "#FFA9FE"),
                Pair("#006A60", "#53DBC9"),
                Pair("#3F5AA6", "#B0C6FF")
            )
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                templates.forEach { (lightHex, darkHex) ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(lightHex)), shape = androidx.compose.foundation.shape.CircleShape)
                            .clickable {
                                viewModel.updateLightColors(lightHex, "#FFFFFF", "#121212")
                                viewModel.updateDarkColors(darkHex, "#121212", "#FFFFFF")
                                Toast.makeText(context, "Skema warna diterapkan", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
            }

            // Light Theme Colors
            Text(stringResource(R.string.light_mode_colors), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        var tempLightPrimary by remember(lightPrimary) { mutableStateOf(lightPrimary) }
        var tempLightSecondary by remember(lightSecondary) { mutableStateOf(lightSecondary) }
        var tempLightText by remember(lightText) { mutableStateOf(lightText) }

        OutlinedTextField(value = tempLightPrimary, onValueChange = { tempLightPrimary = it }, label = { Text(stringResource(R.string.color_primary)) }, placeholder = { Text(stringResource(R.string.hex_color)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        OutlinedTextField(value = tempLightSecondary, onValueChange = { tempLightSecondary = it }, label = { Text(stringResource(R.string.color_secondary)) }, placeholder = { Text(stringResource(R.string.hex_color)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        OutlinedTextField(value = tempLightText, onValueChange = { tempLightText = it }, label = { Text(stringResource(R.string.color_text)) }, placeholder = { Text(stringResource(R.string.hex_color)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

        Button(onClick = { 
            viewModel.updateLightColors(tempLightPrimary, tempLightSecondary, tempLightText) 
            Toast.makeText(context, "Warna khusus tersimpan", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(stringResource(R.string.save))
        }

        // Dark Theme Colors
        Text(stringResource(R.string.dark_mode_colors), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        var tempDarkPrimary by remember(darkPrimary) { mutableStateOf(darkPrimary) }
        var tempDarkSecondary by remember(darkSecondary) { mutableStateOf(darkSecondary) }
        var tempDarkText by remember(darkText) { mutableStateOf(darkText) }

        OutlinedTextField(value = tempDarkPrimary, onValueChange = { tempDarkPrimary = it }, label = { Text(stringResource(R.string.color_primary)) }, placeholder = { Text(stringResource(R.string.hex_color)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        OutlinedTextField(value = tempDarkSecondary, onValueChange = { tempDarkSecondary = it }, label = { Text(stringResource(R.string.color_secondary)) }, placeholder = { Text(stringResource(R.string.hex_color)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        OutlinedTextField(value = tempDarkText, onValueChange = { tempDarkText = it }, label = { Text(stringResource(R.string.color_text)) }, placeholder = { Text(stringResource(R.string.hex_color)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

        Button(onClick = { 
            viewModel.updateDarkColors(tempDarkPrimary, tempDarkSecondary, tempDarkText) 
            Toast.makeText(context, "Warna khusus tersimpan", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(stringResource(R.string.save))
        }
        } // End of if (!useDynamicColor)

        Text(
            text = stringResource(R.string.app_credits),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        } // End Column
    } // End Scaffold
} // End function
