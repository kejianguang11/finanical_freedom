package com.financial.freedom.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ===== Raw Color Values (must be before ColorSchemes) =====
private val Color_White = Color(0xFFFFFFFF)
private val Color_Primary = Color(0xFF1C1917)
private val Color_Secondary = Color(0xFF44403C)
private val Color_Gold = Color(0xFFB7930A)
private val Color_GoldLight = Color(0xFFFEF9E7)
private val Color_Up = FinancialColors.up
private val Color_UpBg = FinancialColors.upBg
private val Color_PageBg = FinancialColors.pageBg
private val Color_SurfaceVariant = Color(0xFFF0EFEC)
private val Color_TextMuted = Color(0xFF6B6560)
private val Color_Border = Color(0xFFE0DDD8)
private val Color_DarkBg = Color(0xFF0F172A)
private val Color_DarkSurface = Color(0xFF1E293B)
private val Color_DarkText = Color(0xFFF8FAFC)
private val Color_DarkMuted = Color(0xFF94A3B8)
private val Color_DarkGold = Color(0xFFF59E0B)

// ===== Premium Light Color Scheme =====
private val LightColorScheme = lightColorScheme(
    primary = Color_Primary,
    onPrimary = Color_White,
    primaryContainer = Color_SurfaceVariant,
    onPrimaryContainer = Color_Primary,
    secondary = Color_Secondary,
    onSecondary = Color_White,
    secondaryContainer = Color_SurfaceVariant,
    onSecondaryContainer = Color_Secondary,
    tertiary = Color_Gold,
    onTertiary = Color_White,
    tertiaryContainer = Color_GoldLight,
    onTertiaryContainer = Color_Primary,
    error = Color_Up,
    errorContainer = Color_UpBg,
    onErrorContainer = Color_Up,
    background = Color_PageBg,
    onBackground = Color_Primary,
    surface = Color_White,
    onSurface = Color_Primary,
    surfaceVariant = Color_SurfaceVariant,
    onSurfaceVariant = Color_TextMuted,
    outline = Color_Border,
    outlineVariant = Color_SurfaceVariant
)

// ===== Premium Dark Color Scheme =====
private val DarkColorScheme = darkColorScheme(
    primary = Color_DarkText,
    onPrimary = Color_DarkBg,
    primaryContainer = Color_DarkSurface,
    onPrimaryContainer = Color_DarkText,
    secondary = Color_DarkMuted,
    onSecondary = Color_DarkBg,
    secondaryContainer = Color_DarkSurface,
    onSecondaryContainer = Color_DarkText,
    tertiary = Color_DarkGold,
    onTertiary = Color_DarkBg,
    tertiaryContainer = Color(0xFF3D2E00),
    onTertiaryContainer = Color_DarkGold,
    error = Color(0xFFEF4444),
    errorContainer = Color(0x33EF4444),
    onErrorContainer = Color(0xFFEF4444),
    background = Color_DarkBg,
    onBackground = Color_DarkText,
    surface = Color_DarkSurface,
    onSurface = Color_DarkText,
    surfaceVariant = Color(0xFF293548),
    onSurfaceVariant = Color_DarkMuted,
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF293548)
)

// ===== Premium Typography =====
private val FinancialTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)

// ===== Theme Composable =====
@Composable
fun FinancialFreedomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FinancialTypography,
        content = content
    )
}
