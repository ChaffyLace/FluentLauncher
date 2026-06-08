/**
 * @file GesturesPage.kt
 * @description Settings page for system gestures and notification access.
 * Manages the SYSTEM_ALERT_WINDOW permission flow for the global Action Center overlay.
 */

package com.example.fluentui.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import fluent.ui.system.icons.regular.Alert

@Composable
fun GesturesPage(
    state: SettingsState,
    onSetShowBadges: (Boolean) -> Unit,
    onSetSwipeDown: (Boolean) -> Unit,
    onSetDoubleTap: (Boolean) -> Unit,
    onSetGlobalActionCenter: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val titleColor = if (isDark) Color.White else Color(0xFF191919)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_gestures_title),
            color = titleColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )

        // ── Notifications ─────────────────────────────────────────────────────
        SectionBlock(stringResource(R.string.settings_section_notifications), isDark) {
            FluentSettingItem(
                icon = FluentIcons.Regular.Alert,
                title = stringResource(R.string.settings_notif_access_title),
                subtitle = stringResource(R.string.settings_notif_access_desc),
                isLast = false,
                onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.error_settings_not_found),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            HorizontalDivider(
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            RowToggleItem(
                title = stringResource(R.string.settings_notif_badges_title),
                subtitle = stringResource(R.string.settings_notif_badges_desc),
                checked = state.showBadges,
                accentColor = state.accentColor,
                onCheckedChange = onSetShowBadges
            )
        }

        // ── Gestes & Système ──────────────────────────────────────────────────
        SectionBlock(stringResource(R.string.settings_section_gestures), isDark) {
            RowToggleItem(
                title = stringResource(R.string.settings_gesture_doubletap_title),
                subtitle = stringResource(R.string.settings_gesture_doubletap_desc),
                checked = state.doubleTapToSleep,
                accentColor = state.accentColor,
                onCheckedChange = onSetDoubleTap
            )

            HorizontalDivider(
                color = dividerColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Global Action Center Toggle
            RowToggleItem(
                title = "Centre de Contrôle Global",
                subtitle = if (state.globalActionCenterEnabled) "Actif sur toutes les applications" else "Désactivé",
                checked = state.globalActionCenterEnabled,
                accentColor = state.accentColor,
                onCheckedChange = { isChecked ->
                    // Guard: Check for SYSTEM_ALERT_WINDOW permission before enabling
                    if (isChecked && !Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } else {
                        // Permission granted, update state
                        onSetGlobalActionCenter(isChecked)

                        // Start or stop the background service dynamically
                        val serviceIntent = Intent(context, com.example.fluentui.ui.system.SwipeDownService::class.java)
                        if (isChecked) {
                            context.startService(serviceIntent)
                        } else {
                            context.stopService(serviceIntent)
                        }
                    }
                }
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ── Composants internes ───────────────────────────────────────────────────────

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
    val textColor = if (enabled) {
        if (isDark) Color.White else Color(0xFF1D1D1D)
    } else {
        if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
    }
    val subTextColor = if (enabled) {
        if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF5E5E5E)
    } else {
        if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
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