package com.example.easycook.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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

/*
 * Le primaire est l'orange foncé : l'orange vif (#FF9800) est trop clair pour du
 * texte ou des icônes sur fond crème. L'orange vif reste disponible en tertiaire
 * pour les gros boutons d'action, avec un texte brun.
 */
private val LightColorScheme = lightColorScheme(
    primary = OrangeDark,
    onPrimary = White,
    primaryContainer = OrangeLight,
    onPrimaryContainer = OrangeDeep,
    secondary = Basil,
    onSecondary = White,
    secondaryContainer = BasilLight,
    onSecondaryContainer = BasilDark,
    tertiary = Orange,
    onTertiary = Cocoa,
    tertiaryContainer = OrangeLight,
    onTertiaryContainer = OrangeDeep,
    background = Cream,
    onBackground = Cocoa,
    surface = Cream,
    onSurface = Cocoa,
    surfaceVariant = CreamVariant,
    onSurfaceVariant = CocoaLight,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = CreamVariant,
    surfaceContainerHigh = CreamVariant,
    surfaceContainerHighest = OrangeLight,
    outline = Taupe,
    outlineVariant = OrangeLight,
    error = Tomato,
    onError = White,
    errorContainer = TomatoLight,
    onErrorContainer = Tomato
)

private val DarkColorScheme = darkColorScheme(
    primary = OrangeNight,
    onPrimary = Cocoa,
    primaryContainer = OrangeNightContainer,
    onPrimaryContainer = OrangeLight,
    secondary = BasilNight,
    onSecondary = BasilDark,
    secondaryContainer = BasilNightContainer,
    onSecondaryContainer = BasilLight,
    tertiary = Orange,
    onTertiary = Cocoa,
    tertiaryContainer = OrangeNightContainer,
    onTertiaryContainer = OrangeLight,
    background = EspressoBackground,
    onBackground = Latte,
    surface = EspressoBackground,
    onSurface = Latte,
    surfaceVariant = EspressoSurfaceHigh,
    onSurfaceVariant = LatteVariant,
    surfaceContainerLowest = EspressoSurface,
    surfaceContainerLow = EspressoSurface,
    surfaceContainer = EspressoSurfaceHigh,
    surfaceContainerHigh = EspressoSurfaceHigh,
    surfaceContainerHighest = OrangeNightContainer,
    outline = CocoaOutline,
    outlineVariant = CocoaOutlineVariant,
    error = TomatoNight,
    onError = TomatoNightContainer,
    errorContainer = TomatoNightContainer,
    onErrorContainer = TomatoNight
)

private val EasyCookShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EasyCookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
        shapes = EasyCookShapes,
        content = content
    )
}
