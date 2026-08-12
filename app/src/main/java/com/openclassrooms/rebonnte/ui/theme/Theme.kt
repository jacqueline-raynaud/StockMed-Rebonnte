package com.openclassrooms.rebonnte.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Les couleurs dynamiques sont **volontairement absentes**.
 *
 * Elles derivent la palette du fond d'ecran de l'utilisateur : les contrastes
 * deviennent imprevisibles et aucune conformite WCAG ne peut etre garantie.
 * Sur une application ou l'on lit des quantites de medicaments, la lisibilite
 * prime sur la coquetterie — et c'est incompatible avec T-31.
 *
 * [darkTheme] est un parametre et non une lecture directe du systeme : le
 * reglage de l'utilisateur peut forcer un mode, quel que soit celui du
 * telephone.
 */
@Composable
fun RebonnteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}