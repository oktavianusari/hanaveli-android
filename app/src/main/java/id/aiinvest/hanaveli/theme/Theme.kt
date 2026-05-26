package id.aiinvest.hanaveli.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = M3PrimaryDark,
    secondary = M3SecondaryDark,
    tertiary = M3TertiaryDark,
    background = M3BackgroundDark,
    surface = M3SurfaceDark,
    onPrimary = M3OnPrimaryDark,
    onBackground = M3OnBackgroundDark,
    onSurface = M3OnBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = M3PrimaryLight,
    secondary = M3SecondaryLight,
    tertiary = M3TertiaryLight,
    background = M3BackgroundLight,
    surface = M3SurfaceLight,
    onPrimary = M3OnPrimaryLight,
    onBackground = M3OnBackgroundLight,
    onSurface = M3OnBackgroundLight
)

@Composable
fun ValasMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic color to enforce pastel theme
    lightPrimaryHex: String = "#386A20",
    lightSecondaryHex: String = "#D7E8CD",
    lightTextHex: String = "#1A1C18",
    darkPrimaryHex: String = "#9CD67D",
    darkSecondaryHex: String = "#BBCBB1",
    darkTextHex: String = "#E3E3DC",
    content: @Composable () -> Unit
) {
    fun parseColor(hex: String, fallback: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
        return try {
            val hexStr = if (!hex.startsWith("#")) "#$hex" else hex
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hexStr))
        } catch (e: Exception) {
            fallback
        }
    }

    val customLightColorScheme = lightColorScheme(
        primary = parseColor(lightPrimaryHex, M3PrimaryLight),
        secondary = parseColor(lightSecondaryHex, M3SecondaryLight),
        tertiary = M3TertiaryLight,
        background = M3BackgroundLight,
        surface = M3SurfaceLight,
        onPrimary = parseColor(lightTextHex, M3OnPrimaryLight),
        onBackground = parseColor(lightTextHex, M3OnBackgroundLight),
        onSurface = parseColor(lightTextHex, M3OnBackgroundLight)
    )

    val customDarkColorScheme = darkColorScheme(
        primary = parseColor(darkPrimaryHex, M3PrimaryDark),
        secondary = parseColor(darkSecondaryHex, M3SecondaryDark),
        tertiary = M3TertiaryDark,
        background = M3BackgroundDark,
        surface = M3SurfaceDark,
        onPrimary = parseColor(darkTextHex, M3OnPrimaryDark),
        onBackground = parseColor(darkTextHex, M3OnBackgroundDark),
        onSurface = parseColor(darkTextHex, M3OnBackgroundDark)
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> customDarkColorScheme
        else -> customLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
