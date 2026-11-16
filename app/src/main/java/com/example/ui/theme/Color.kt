package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.model.FolderColor

// Light editorial palette — Warm Alabaster & Luminous Cobalt
val LightBackground = Color(0xFFFAF9F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0EEE7)
val LightSurfaceContainerHigh = Color(0xFFE7E4DA)
val LightPrimary = Color(0xFF4252F5)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE2E5FF)
val LightOnPrimaryContainer = Color(0xFF0E1768)
val LightSecondary = Color(0xFF0D8A9E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFCFF4FA)
val LightOnSecondaryContainer = Color(0xFF033842)
val LightTertiary = Color(0xFFD97706)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFEF0D6)
val LightOnTertiaryContainer = Color(0xFF472300)
val LightOnBackground = Color(0xFF16192B)
val LightOnSurface = Color(0xFF16192B)
val LightOnSurfaceVariant = Color(0xFF565B73)
val LightOutline = Color(0xFFC9C7BE)
val LightOutlineVariant = Color(0xFFE4E2D9)
val LightError = Color(0xFFDC2626)

// Dark editorial palette — Deep Midnight Indigo & Electric Periwinkle
val DarkBackground = Color(0xFF0E111E)
val DarkSurface = Color(0xFF161A2C)
val DarkSurfaceVariant = Color(0xFF20253D)
val DarkSurfaceContainerHigh = Color(0xFF282E4B)
val DarkPrimary = Color(0xFF818EFF)
val DarkOnPrimary = Color(0xFF09135C)
val DarkPrimaryContainer = Color(0xFF2633A8)
val DarkOnPrimaryContainer = Color(0xFFE2E5FF)
val DarkSecondary = Color(0xFF4FD1E8)
val DarkOnSecondary = Color(0xFF003640)
val DarkSecondaryContainer = Color(0xFF094E5C)
val DarkOnSecondaryContainer = Color(0xFFCFF4FA)
val DarkTertiary = Color(0xFFFBBF24)
val DarkOnTertiary = Color(0xFF452800)
val DarkTertiaryContainer = Color(0xFF633B00)
val DarkOnTertiaryContainer = Color(0xFFFEF0D6)
val DarkOnBackground = Color(0xFFF3F4FA)
val DarkOnSurface = Color(0xFFF3F4FA)
val DarkOnSurfaceVariant = Color(0xFFA7ACC4)
val DarkOutline = Color(0xFF3A4161)
val DarkOutlineVariant = Color(0xFF262C46)
val DarkError = Color(0xFFF87171)

data class FolderSwatch(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
    val border: Color
)

fun folderSwatch(color: FolderColor, isDark: Boolean): FolderSwatch {
    return when (color) {
        FolderColor.COBALT -> if (isDark) {
            FolderSwatch(Color(0xFF818EFF), Color(0xFF1E2656), Color(0xFFE0E4FF), Color(0xFF394696))
        } else {
            FolderSwatch(Color(0xFF4252F5), Color(0xFFEEF0FF), Color(0xFF172280), Color(0xFFC7CEFF))
        }
        FolderColor.AMBER -> if (isDark) {
            FolderSwatch(Color(0xFFFBBF24), Color(0xFF3C2B08), Color(0xFFFEF3C7), Color(0xFF78530E))
        } else {
            FolderSwatch(Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFF78350F), Color(0xFFFDE68A))
        }
        FolderColor.EMERALD -> if (isDark) {
            FolderSwatch(Color(0xFF34D399), Color(0xFF0A3327), Color(0xFFD1FAE5), Color(0xFF13634B))
        } else {
            FolderSwatch(Color(0xFF059669), Color(0xFFECFDF5), Color(0xFF064E3B), Color(0xFFA7F3D0))
        }
        FolderColor.VIOLET -> if (isDark) {
            FolderSwatch(Color(0xFFA78BFA), Color(0xFF2A1D52), Color(0xFFEDE9FE), Color(0xFF53399C))
        } else {
            FolderSwatch(Color(0xFF7C3AED), Color(0xFFF3EEFF), Color(0xFF4C1D95), Color(0xFFDDD6FE))
        }
        FolderColor.CORAL -> if (isDark) {
            FolderSwatch(Color(0xFFFB7185), Color(0xFF421622), Color(0xFFFFE4E6), Color(0xFF882941))
        } else {
            FolderSwatch(Color(0xFFE11D48), Color(0xFFFFF1F2), Color(0xFF881337), Color(0xFFFECDD3))
        }
        FolderColor.TEAL -> if (isDark) {
            FolderSwatch(Color(0xFF2DD4BF), Color(0xFF0B3534), Color(0xFFCCFBF1), Color(0xFF156663))
        } else {
            FolderSwatch(Color(0xFF0D9488), Color(0xFFF0FDFA), Color(0xFF134E4A), Color(0xFF99F6E4))
        }
    }
}
