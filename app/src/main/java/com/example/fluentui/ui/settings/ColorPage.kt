/**
 * @file ColorPage.kt
 * @description Windows 11 Accent Color and Transparency settings.
 * Features: Material Mica effects toggle and full Fluent color palette.
 * Engineering Note: Optimized Grid for large color sets and fully localized.
 */

package com.example.fluentui.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.R
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Sparkle

@Composable
fun ColorPage(
    state: SettingsState,
    onSetAccent: (Color) -> Unit,
    onSetTransparency: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // UI Theme tokens
    val titleColor = if (isDark) Color.White else Color(0xFF191919)
    val textColor = if (isDark) Color.White else Color(0xFF1D1D1D)
    val subTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF606060)

    // Engineering Note: Memoize the color list to prevent unnecessary allocations
    val windowsColors = remember {
        listOf(
            Color(0xFFFFB900), Color(0xFFFF8C00), Color(0xFFF7630C), Color(0xFFCA5010),
            Color(0xFFDA3B01), Color(0xFFEF6950), Color(0xFFD13438), Color(0xFFFF4343),
            Color(0xFFE74856), Color(0xFFE81123), Color(0xFFEA005E), Color(0xFFC30052),
            Color(0xFFE3008C), Color(0xFFBF0077), Color(0xFFC239B3), Color(0xFF9A0089),
            Color(0xFF0078D7), Color(0xFF0063B1), Color(0xFF8E8CD8), Color(0xFF6B69D6),
            Color(0xFF8764B8), Color(0xFF744DA9), Color(0xFFB146C2), Color(0xFF881798),
            Color(0xFF0099BC), Color(0xFF2D7D9A), Color(0xFF00B7C3), Color(0xFF038387),
            Color(0xFF00B294), Color(0xFF018574), Color(0xFF00CC6A), Color(0xFF10893E),
            Color(0xFF7A7574), Color(0xFF5D5A58), Color(0xFF68768A), Color(0xFF515C6B),
            Color(0xFF567C73), Color(0xFF486860), Color(0xFF498205), Color(0xFF107C10)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.settings_colors_title),
            color = titleColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(20.dp))

        // --- TRANSPARENCY CARD ---
        FluentCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FluentIcons.Regular.Sparkle,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_colors_transparency),
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.settings_colors_mica_desc),
                        color = subTextColor,
                        fontSize = 12.sp
                    )
                }
                WindowsSwitch(
                    checked = state.transparencyEnabled,
                    onCheckedChange = onSetTransparency,
                    enabled = true,
                    accentColor = state.accentColor
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- COLOR PALETTE SECTION ---
        Text(
            text = stringResource(R.string.settings_colors_windows_palette),
            color = titleColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        FluentCard(padding = 16.dp) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 44.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                items(windowsColors) { color ->
                    WindowsColorSwatch(
                        color = color,
                        isSelected = color == state.accentColor,
                        onClick = { onSetAccent(color) }
                    )
                }
            }
        }
    }
}