package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class AcademicColors(
    val overdue: Color,
    val overdueContainer: Color,
    val dueSoon: Color,
    val dueSoonContainer: Color,
    val completed: Color,
    val completedContainer: Color,
    val assignmentAccent: Color,
    val testAccent: Color,
    val presentationAccent: Color,
    val examAccent: Color
)

private val LightAcademicColors = AcademicColors(
    overdue = OverdueRed,
    overdueContainer = OverdueRedContainer,
    dueSoon = DueSoonAmber,
    dueSoonContainer = DueSoonAmberContainer,
    completed = CompletedSage,
    completedContainer = CompletedSageContainer,
    assignmentAccent = TypeAssignmentAccent,
    testAccent = TypeTestAccent,
    presentationAccent = TypePresentationAccent,
    examAccent = TypeExamAccent
)

private val DarkAcademicColors = AcademicColors(
    overdue = OverdueRedDark,
    overdueContainer = OverdueRedDarkContainer,
    dueSoon = DueSoonAmberDark,
    dueSoonContainer = DueSoonAmberDarkContainer,
    completed = CompletedSageDark,
    completedContainer = CompletedSageDarkContainer,
    assignmentAccent = Color(0xFF5BC0D4),
    testAccent = Color(0xFF93A5FF),
    presentationAccent = Color(0xFFF48FB1),
    examAccent = Color(0xFF4DD0B8)
)

val LocalAcademicColors = staticCompositionLocalOf { LightAcademicColors }

private val DarkColorScheme = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF002630),
    primaryContainer = TealLightContainer,
    onPrimaryContainer = OnTealLightContainer,
    secondary = AmberLight,
    onSecondary = Color(0xFF3A1D00),
    secondaryContainer = AmberDarkContainer,
    onSecondaryContainer = OnAmberDarkContainer,
    tertiary = Color(0xFF81C784),
    background = ObsidianBackground,
    onBackground = ChalkPrimary,
    surface = ObsidianSurface,
    onSurface = ChalkPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ChalkSecondary,
    outline = ObsidianOutline,
    error = OverdueRedDark,
    errorContainer = OverdueRedDarkContainer
)

private val LightColorScheme = lightColorScheme(
    primary = CollegiateTeal,
    onPrimary = Color.White,
    primaryContainer = CollegiateTealContainer,
    onPrimaryContainer = OnCollegiateTealContainer,
    secondary = WarmAmber,
    onSecondary = Color.White,
    secondaryContainer = WarmAmberContainer,
    onSecondaryContainer = OnWarmAmberContainer,
    tertiary = CompletedSage,
    background = ParchmentBackground,
    onBackground = InkPrimary,
    surface = ParchmentSurface,
    onSurface = InkPrimary,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = ParchmentOutline,
    error = OverdueRed,
    errorContainer = OverdueRedContainer
)

@Composable
fun ScholarPaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val academicColors = if (darkTheme) DarkAcademicColors else LightAcademicColors

    CompositionLocalProvider(LocalAcademicColors provides academicColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
