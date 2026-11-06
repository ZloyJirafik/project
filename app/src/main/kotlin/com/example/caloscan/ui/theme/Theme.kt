package com.example.caloscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.caloscan.R

/** App colors that Material 3 has no name for (macro colors, card border...). */
@Immutable
data class CaloColors(
  val card: Color,
  val muted: Color,
  val border: Color,
  val accentSoft: Color,
  val protein: Color,
  val carbs: Color,
  val fat: Color,
  val good: Color,
  val danger: Color,
  val ringTrack: Color,
)

private val LightCaloColors = CaloColors(
  card = Color(0xFFFFFFFF),
  muted = Color(0xFF6E6B64),
  border = Color(0xFFE4E1DA),
  accentSoft = Color(0xFFFBE5DC),
  protein = Color(0xFF3D7BE0),
  carbs = Color(0xFFD99A12),
  fat = Color(0xFFCC4F8C),
  good = Color(0xFF2B9A60),
  danger = Color(0xFFD43F3A),
  ringTrack = Color(0xFFEEEBE4),
)

private val DarkCaloColors = CaloColors(
  card = Color(0xFF1B1B19),
  muted = Color(0xFFA39F96),
  border = Color(0xFF2F2E2A),
  accentSoft = Color(0xFF3A2118),
  protein = Color(0xFF6EA2FF),
  carbs = Color(0xFFF0B735),
  fat = Color(0xFFEC7FB5),
  good = Color(0xFF4CC184),
  danger = Color(0xFFFF6B66),
  ringTrack = Color(0xFF2A2926),
)

private val LightScheme = lightColorScheme(
  primary = Color(0xFFE4572E),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFBE5DC),
  onPrimaryContainer = Color(0xFFC8441D),
  secondary = Color(0xFF6E6B64),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFFBE5DC),
  onSecondaryContainer = Color(0xFFC8441D),
  background = Color(0xFFF4F3EF),
  onBackground = Color(0xFF1D1C1A),
  surface = Color(0xFFF4F3EF),
  onSurface = Color(0xFF1D1C1A),
  surfaceVariant = Color(0xFFF0EEE9),
  onSurfaceVariant = Color(0xFF6E6B64),
  surfaceContainerLowest = Color.White,
  surfaceContainerLow = Color.White,
  surfaceContainer = Color.White,
  surfaceContainerHigh = Color(0xFFF0EEE9),
  surfaceContainerHighest = Color(0xFFE9E6DF),
  outline = Color(0xFFCBC7BE),
  outlineVariant = Color(0xFFE4E1DA),
  error = Color(0xFFD43F3A),
  onError = Color.White,
  inverseSurface = Color(0xFF2B2A27),
  inverseOnSurface = Color(0xFFF4F3EF),
  inversePrimary = Color(0xFFFF8763),
)

private val DarkScheme = darkColorScheme(
  primary = Color(0xFFFF6A3D),
  onPrimary = Color(0xFF1A0D07),
  primaryContainer = Color(0xFF3A2118),
  onPrimaryContainer = Color(0xFFFF8763),
  secondary = Color(0xFFA39F96),
  onSecondary = Color(0xFF111110),
  secondaryContainer = Color(0xFF3A2118),
  onSecondaryContainer = Color(0xFFFF8763),
  background = Color(0xFF111110),
  onBackground = Color(0xFFF2F0EB),
  surface = Color(0xFF111110),
  onSurface = Color(0xFFF2F0EB),
  surfaceVariant = Color(0xFF272623),
  onSurfaceVariant = Color(0xFFA39F96),
  surfaceContainerLowest = Color(0xFF0C0C0B),
  surfaceContainerLow = Color(0xFF1B1B19),
  surfaceContainer = Color(0xFF1B1B19),
  surfaceContainerHigh = Color(0xFF272623),
  surfaceContainerHighest = Color(0xFF302F2B),
  outline = Color(0xFF55524B),
  outlineVariant = Color(0xFF2F2E2A),
  error = Color(0xFFFF6B66),
  onError = Color(0xFF1A0D07),
  inverseSurface = Color(0xFFF2F0EB),
  inverseOnSurface = Color(0xFF1B1B19),
  inversePrimary = Color(0xFFE4572E),
)

/** Inter (res/font, SIL Open Font License). Has Cyrillic letters. */
val Inter = FontFamily(
  Font(R.font.inter_regular, FontWeight.Normal),
  Font(R.font.inter_medium, FontWeight.Medium),
  Font(R.font.inter_semibold, FontWeight.SemiBold),
  Font(R.font.inter_bold, FontWeight.Bold),
)

/** The default Material 3 text styles, all set in Inter. */
private val CaloTypography: Typography = Typography().let { t ->
  Typography(
    displayLarge = t.displayLarge.copy(fontFamily = Inter),
    displayMedium = t.displayMedium.copy(fontFamily = Inter),
    displaySmall = t.displaySmall.copy(fontFamily = Inter),
    headlineLarge = t.headlineLarge.copy(fontFamily = Inter),
    headlineMedium = t.headlineMedium.copy(fontFamily = Inter),
    headlineSmall = t.headlineSmall.copy(fontFamily = Inter),
    titleLarge = t.titleLarge.copy(fontFamily = Inter),
    titleMedium = t.titleMedium.copy(fontFamily = Inter),
    titleSmall = t.titleSmall.copy(fontFamily = Inter),
    bodyLarge = t.bodyLarge.copy(fontFamily = Inter),
    bodyMedium = t.bodyMedium.copy(fontFamily = Inter),
    bodySmall = t.bodySmall.copy(fontFamily = Inter),
    labelLarge = t.labelLarge.copy(fontFamily = Inter),
    labelMedium = t.labelMedium.copy(fontFamily = Inter),
    labelSmall = t.labelSmall.copy(fontFamily = Inter),
  )
}

private val LocalCaloColors = staticCompositionLocalOf { LightCaloColors }

/** Light or dark follows the phone's setting. */
@Composable
fun CaloScanTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(LocalCaloColors provides if (darkTheme) DarkCaloColors else LightCaloColors) {
    MaterialTheme(
      colorScheme = if (darkTheme) DarkScheme else LightScheme,
      typography = CaloTypography,
      content = content,
    )
  }
}

/** Access with CaloTheme.colors.protein etc. */
object CaloTheme {
  val colors: CaloColors
    @Composable @ReadOnlyComposable
    get() = LocalCaloColors.current
}
