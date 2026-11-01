package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = StudioDarkGreen,
    secondary = StudioDarkCyan,
    tertiary = Color(0xFFFFB74D),
    background = StudioDarkBackground,
    surface = StudioDarkSurface,
    surfaceVariant = StudioDarkSurfaceVariant,
    outline = StudioDarkOutline,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = StudioDarkOnSurface,
    onSurface = StudioDarkOnSurface,
    onSurfaceVariant = Color(0xFFA8ADBD)
)

private val LightColorScheme = lightColorScheme(
    primary = StudioLightGreen,
    secondary = StudioLightCyan,
    tertiary = Color(0xFFF57C00),
    background = StudioLightBackground,
    surface = StudioLightSurface,
    surfaceVariant = StudioLightSurfaceVariant,
    outline = StudioLightOutline,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = StudioLightOnSurface,
    onSurface = StudioLightOnSurface,
    onSurfaceVariant = Color(0xFF59636E)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent Android Studio IDE styling
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
