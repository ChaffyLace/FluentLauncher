package com.example.fluentui.ui.homescreen.widgets

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.components.FluentBadgedBox
import com.example.fluentui.ui.components.InternalAppIcon
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.color.*

@Composable
fun StandardAppTile(
    config:            TileConfig,
    contentColor:      Color,
    notificationCount: Int = 0
) {
    val customIcon = remember(config.packageName) { getCustomFluentIcon(config.packageName) }

    val iconSize = when (config.size) {
        TileSize.TINY   -> 28.dp
        TileSize.SMALL  -> 44.dp
        TileSize.MEDIUM -> 52.dp
        TileSize.LARGE  -> 64.dp
    }

    when (config.size) {

        // ── TINY — icône seule, centrée ──────────────────────────────────────
        TileSize.TINY -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SharedAppIcon(config.packageName, customIcon, iconSize, notificationCount)
            }
        }

        // ── SMALL — icône centrée, label en bas ───────────────────────────────
        TileSize.SMALL -> {
            Column(
                modifier            = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SharedAppIcon(config.packageName, customIcon, iconSize, notificationCount)
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = config.title,
                    fontSize   = 11.sp,
                    color      = contentColor,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }

        // ── MEDIUM (4×2) — icône à gauche verticalement centrée, label à droite
        TileSize.MEDIUM -> {
            Row(
                modifier          = Modifier.fillMaxSize().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SharedAppIcon(config.packageName, customIcon, iconSize, notificationCount)
                Spacer(Modifier.width(12.dp))
                Text(
                    text       = config.title,
                    fontSize   = 14.sp,
                    color      = contentColor,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }

        // ── LARGE (4×4) — icône centrée en haut, label en bas à gauche ────────
        TileSize.LARGE -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(12.dp)
            ) {
                SharedAppIcon(
                    packageName       = config.packageName,
                    customIcon        = customIcon,
                    iconSize          = iconSize,
                    notificationCount = notificationCount,
                    modifier          = Modifier.align(Alignment.Center)
                )
                Text(
                    text       = config.title,
                    fontSize   = 14.sp,
                    color      = contentColor,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                    modifier   = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SharedAppIcon(
    packageName:       String,
    customIcon:        ImageVector?,
    iconSize:          androidx.compose.ui.unit.Dp,
    notificationCount: Int,
    modifier:          Modifier = Modifier
) {
    val isDark      = isSystemInDarkTheme()
    val badgeColor  = Color(0xFFC42B1C)
    val cutoutColor = if (isDark) Color.Black.copy(0.2f) else Color.White.copy(0.2f)

    FluentBadgedBox(
        badgeCount  = notificationCount,
        badgeColor  = badgeColor,
        cutoutColor = cutoutColor,
        modifier    = modifier
    ) {
        if (customIcon != null) {
            Icon(
                imageVector        = customIcon,
                contentDescription = null,
                modifier           = Modifier.size(iconSize),
                tint               = Color.Unspecified
            )
        } else {
            InternalAppIcon(packageName = packageName, modifier = Modifier.size(iconSize))
        }
    }
}

private fun getCustomFluentIcon(packageName: String): ImageVector? {
    val pkg = packageName.lowercase()
    return when {
        pkg.contains("calendar")                                                        -> FluentIcons.Color.Calendar
        pkg.contains("camera")                                                          -> FluentIcons.Color.Camera
        pkg.contains("messaging") || pkg.contains("whatsapp") || pkg.contains("sms")   -> FluentIcons.Color.Chat
        pkg.contains("clock") || pkg.contains("alarm")                                 -> FluentIcons.Color.Clock
        pkg.contains("gallery") || pkg.contains("photos")                              -> FluentIcons.Color.Image
        pkg.contains("maps") || pkg.contains("navigation")                             -> FluentIcons.Color.LocationRipple
        pkg.contains("mail") || pkg.contains("outlook")                                -> FluentIcons.Color.Mail
        pkg.contains("contacts") || pkg.contains("people")                             -> FluentIcons.Color.Person
        pkg.contains("browser") || pkg.contains("chrome") || pkg.contains("firefox")   -> FluentIcons.Color.Planet
        pkg.contains("settings") || pkg.contains("config")                             -> FluentIcons.Color.Settings
        pkg.contains("video") || pkg.contains("youtube") || pkg.contains("player")     -> FluentIcons.Color.Video
        pkg.contains("note") || pkg.contains("keep")                                   -> FluentIcons.Color.Notebook
        pkg.contains("file") || pkg.contains("explorer") || pkg.contains("download")   -> FluentIcons.Color.DocumentFolder
        else                                                                            -> null
    }
}