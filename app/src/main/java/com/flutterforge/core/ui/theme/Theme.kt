package com.flutterforge.core.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = FlutterCyan,
    onPrimary = OnFlutterCyan,
    primaryContainer = FlutterCyanContainer,
    onPrimaryContainer = OnFlutterCyanContainer,
    secondary = ForgeIndigo,
    onSecondary = OnForgeIndigo,
    secondaryContainer = ForgeIndigoContainer,
    onSecondaryContainer = OnForgeIndigoContainer,
    tertiary = FlutterEmerald,
    tertiaryContainer = FlutterEmeraldContainer,
    onTertiaryContainer = FlutterEmerald,
    background = ObsidianBg,
    onBackground = InkPrimary,
    surface = ForgeSurface,
    onSurface = InkPrimary,
    surfaceVariant = ForgeSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    surfaceContainerHigh = ForgeSurfaceHigh,
    outline = ForgeOutline,
    outlineVariant = ForgeOutline,
    error = RoseError,
    errorContainer = RoseErrorContainer,
    onErrorContainer = RoseError
)

private val LightColorScheme = lightColorScheme(
    primary = LightFlutterBlue,
    onPrimary = LightSurface,
    primaryContainer = LightFlutterBlueContainer,
    onPrimaryContainer = LightInkPrimary,
    secondary = LightIndigo,
    onSecondary = LightSurface,
    tertiary = FlutterEmerald,
    background = LightBg,
    onBackground = LightInkPrimary,
    surface = LightSurface,
    onSurface = LightInkPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightInkSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline
)

val FlutterForgeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FlutterForgeTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FlutterForgeTypography,
        shapes = FlutterForgeShapes,
        content = content
    )
}
