package ch.parkassist.app.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButtonDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

private val Background = Color(0xFF080808)
private val SurfaceColor = Color(0xFF111111)
private val ElevatedSurface = Color(0xFF191919)
private val PrimaryRed = Color(0xFFF3161B)
private val DarkRed = Color(0xFFA90008)
private val PrimaryText = Color(0xFFF5F5F5)
private val SecondaryText = Color(0xFFA8A8A8)
private val Divider = Color(0xFF343434)
private val SuccessGreen = Color(0xFF2DBE72)
private val WarningAmber = Color(0xFFFFB74D)
private val NeutralGraphite = Color(0xFF4B4B4B)

private val ParkingColorScheme = darkColorScheme(
    primary = PrimaryRed,
    onPrimary = Color.White,
    primaryContainer = DarkRed,
    onPrimaryContainer = PrimaryText,
    secondary = Color(0xFFDDDDDD),
    onSecondary = Background,
    tertiary = WarningAmber,
    onTertiary = Background,
    background = Background,
    onBackground = PrimaryText,
    surface = SurfaceColor,
    onSurface = PrimaryText,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = SecondaryText,
    surfaceTint = PrimaryRed,
    error = DarkRed,
    onError = PrimaryText,
    errorContainer = Color(0xFF3A0909),
    onErrorContainer = PrimaryText,
    outline = Divider,
    outlineVariant = NeutralGraphite,
    inverseSurface = PrimaryText,
    inverseOnSurface = Background,
    inversePrimary = DarkRed,
)

private val ParkingTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp,
    ),
)

private val ParkingShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Immutable
data class ParkingStatusColors(
    val active: Color,
    val waiting: Color,
    val error: Color,
    val neutral: Color,
)

private val LocalParkingStatusColors = staticCompositionLocalOf {
    ParkingStatusColors(
        active = SuccessGreen,
        waiting = WarningAmber,
        error = PrimaryRed,
        neutral = NeutralGraphite,
    )
}

object ParkingThemeTokens {
    val statusColors: ParkingStatusColors
        @Composable get() = LocalParkingStatusColors.current
}

@Composable
fun ParkingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ParkingColorScheme,
        typography = ParkingTypography,
        shapes = ParkingShapes,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalParkingStatusColors provides ParkingStatusColors(
                active = SuccessGreen,
                waiting = WarningAmber,
                error = PrimaryRed,
                neutral = NeutralGraphite,
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    }
}

@Composable
fun parkingTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.primary,
)

@Composable
fun parkingPrimaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun parkingDestructiveButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.error,
    contentColor = MaterialTheme.colorScheme.onError,
    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun parkingTextButtonColors() = TextButtonDefaults.textButtonColors(
    contentColor = MaterialTheme.colorScheme.primary,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun parkingOutlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
fun parkingCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = MaterialTheme.colorScheme.primary,
    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
)

fun ColorScheme.statusContainer(accent: Color): Color = accent.copy(alpha = 0.16f)
