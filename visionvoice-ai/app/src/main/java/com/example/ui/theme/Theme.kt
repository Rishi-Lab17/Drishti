package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.local.ContrastTheme

private val DarkCyanColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color.Black,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = CyanAccent,
    secondary = CyanSecondary,
    onSecondary = Color.Black,
    tertiary = LuminousYellow,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextHighWhite,
    surface = DarkSurface,
    onSurface = TextHighWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMediumWhite,
    error = DangerRed,
    onError = Color.White
)

private val YellowOnBlackColorScheme = darkColorScheme(
    primary = TaxiYellow,
    onPrimary = Color.Black,
    primaryContainer = PureBlackSurface,
    onPrimaryContainer = TaxiYellow,
    secondary = TaxiYellowDim,
    onSecondary = Color.Black,
    tertiary = Color.White,
    onTertiary = Color.Black,
    background = PureBlack,
    onBackground = TaxiYellow,
    surface = PureBlackSurface,
    onSurface = TaxiYellow,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color.White,
    error = Color(0xFFFF1744),
    onError = Color.White
)

private val AmberOnBlackColorScheme = darkColorScheme(
    primary = WarmAmber,
    onPrimary = Color.Black,
    primaryContainer = PureBlackSurface,
    onPrimaryContainer = WarmAmber,
    secondary = WarmAmberDim,
    onSecondary = Color.Black,
    tertiary = Color.White,
    onTertiary = Color.Black,
    background = PureBlack,
    onBackground = WarmAmber,
    surface = PureBlackSurface,
    onSurface = WarmAmber,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color.White,
    error = Color(0xFFFF3D00),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BrightBlue,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = DeepNavy,
    secondary = DeepNavy,
    onSecondary = Color.White,
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = DeepNavy,
    surface = LightSurface,
    onSurface = DeepNavy,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF334155),
    error = DangerRed,
    onError = Color.White
)

@Composable
fun VisionVoiceTheme(
    contrastTheme: ContrastTheme = ContrastTheme.DARK_CYAN,
    content: @Composable () -> Unit
) {
    val colorScheme = when (contrastTheme) {
        ContrastTheme.DARK_CYAN -> DarkCyanColorScheme
        ContrastTheme.YELLOW_ON_BLACK -> YellowOnBlackColorScheme
        ContrastTheme.AMBER_ON_BLACK -> AmberOnBlackColorScheme
        ContrastTheme.LIGHT_HIGH_CONTRAST -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
