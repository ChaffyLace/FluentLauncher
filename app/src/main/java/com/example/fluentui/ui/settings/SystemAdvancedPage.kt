/**
 * @file SystemAdvancedPage.kt
 * @description Advanced settings page for intents, backups, and security.
 * Engineering Note: Pre-resolves string resources at the Composable level
 * to strictly comply with Compose Lint rules and prevent memory leaks.
 */

package com.example.fluentui.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.R
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.CheckmarkCircle
import fluent.ui.system.icons.regular.FolderOpen
import fluent.ui.system.icons.regular.Save

@Composable
fun SystemAdvancedPage(
    state: SettingsState,
    onSetSecureHiddenApps: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color.White else Color(0xFF191919)

    // Pre-resolve strings to strictly comply with Compose Lint rules
    // Avoids "Querying resource values using LocalContext.current" warning
    val errorNotFoundMsg = stringResource(R.string.error_setting_not_found)
    val biometricRequiredMsg = stringResource(R.string.biometric_required_toast)
    val featureBetaMsg = stringResource(R.string.feature_beta_toast)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- PAGE HEADER ---
        Text(
            text = stringResource(R.string.settings_system_title),
            color = titleColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )

        // --- SECTION: ANDROID INTEGRATION ---
        SectionBlock(stringResource(R.string.settings_section_android), isDark) {
            FluentSettingItem(
                icon = FluentIcons.Regular.CheckmarkCircle,
                title = stringResource(R.string.settings_default_launcher_title),
                subtitle = stringResource(R.string.settings_default_launcher_desc),
                isLast = true,
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, errorNotFoundMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // --- SECTION: SECURITY ---
        SectionBlock(stringResource(R.string.settings_section_security), isDark) {
            RowToggleItem(
                title = stringResource(R.string.settings_secure_apps_title),
                subtitle = stringResource(R.string.settings_secure_apps_desc),
                checked = state.secureHiddenApps,
                accentColor = state.accentColor,
                onCheckedChange = { enabled ->
                    Toast.makeText(context, biometricRequiredMsg, Toast.LENGTH_SHORT).show()
                    onSetSecureHiddenApps(enabled)
                }
            )
        }

        // --- SECTION: BACKUP & RESTORE ---
        SectionBlock(stringResource(R.string.settings_section_backup), isDark) {
            FluentSettingItem(
                icon = FluentIcons.Regular.Save,
                title = stringResource(R.string.settings_backup_config_title),
                subtitle = stringResource(R.string.settings_backup_config_desc),
                isLast = false,
                onClick = { Toast.makeText(context, featureBetaMsg, Toast.LENGTH_SHORT).show() }
            )
            FluentSettingItem(
                icon = FluentIcons.Regular.FolderOpen,
                title = stringResource(R.string.settings_restore_config_title),
                subtitle = stringResource(R.string.settings_restore_config_desc),
                isLast = true,
                onClick = { Toast.makeText(context, featureBetaMsg, Toast.LENGTH_SHORT).show() }
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

// =============================================================================
// PRIVATE UI UTILITIES
// =============================================================================

@Composable
private fun SectionBlock(
    title: String,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            color = if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFFAAAAAA),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        FluentCard(padding = 4.dp, content = content)
    }
}

@Composable
private fun RowToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val textColor = when {
        !enabled -> if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
        isDark -> Color.White
        else -> Color(0xFF1D1D1D)
    }

    val subTextColor = when {
        !enabled -> if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
        isDark -> Color.White.copy(alpha = 0.6f)
        else -> Color(0xFF5E5E5E)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = subTextColor, fontSize = 12.sp)
        }
        WindowsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            accentColor = accentColor
        )
    }
}