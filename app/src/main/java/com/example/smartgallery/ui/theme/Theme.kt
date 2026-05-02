package com.example.smartgallery.ui.theme

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
    primary = Sand80,
    secondary = Mint80,
    tertiary = Sky80,
    background = Ink40,
    surface = Ink40,
    onPrimary = Ink40,
    onSecondary = Ink40,
    onTertiary = Ink40,
    onBackground = Sand80,
    onSurface = Sand80
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    secondary = Coral40,
    tertiary = Sky80,
    background = Color(0xFFF7F4EF),
    surface = Color(0xFFFDFBF8),
    surfaceVariant = Color(0xFFE9E4DB),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Ink40,
    onBackground = Ink40,
    onSurface = Ink40,
    onSurfaceVariant = Color(0xFF5F6570)
)

@Composable
fun SmartGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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