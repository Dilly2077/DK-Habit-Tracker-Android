package com.dk.habittracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.dk.habittracker.data.ThemeMode

private val Seed = Color(0xFF36D07C)
private val DarkScheme = darkColorScheme(
    primary = Seed,
    secondary = Color(0xFF76D7A5),
    tertiary = Color(0xFF8EDBC0),
    background = Color(0xFF101512),
    surface = Color(0xFF161C18)
)
private val OledScheme = darkColorScheme(
    primary = Color(0xFF45E58B),
    secondary = Color(0xFF7BE0A8),
    tertiary = Color(0xFFA1E8C0),
    background = Color.Black,
    surface = Color(0xFF080B09),
    surfaceVariant = Color(0xFF111713)
)
private val LightScheme = lightColorScheme(
    primary = Color(0xFF08793F),
    secondary = Color(0xFF3C7757),
    tertiary = Color(0xFF2B765C),
    background = Color(0xFFF8FBF7),
    surface = Color.White
)

@Composable
fun DKHabitTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED -> true
    }
    val context = LocalContext.current
    val scheme = when {
        mode == ThemeMode.OLED -> OledScheme
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mode == ThemeMode.SYSTEM ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
}
