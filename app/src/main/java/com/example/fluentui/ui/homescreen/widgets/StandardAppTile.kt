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
    config: TileConfig,
    contentColor: Color,
    notificationCount: Int = 0
) {
    val customIcon = remember(config.packageName) {
        getCustomFluentIcon(config.packageName)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp) // padding global léger
    ) {

        val tileWidth = maxWidth
        val tileHeight = maxHeight

        val iconSize = minOf(tileWidth, tileHeight) * when (config.size) {
            TileSize.TINY   -> 0.60f
            TileSize.SMALL  -> 0.40f
            TileSize.MEDIUM -> 0.40f
            TileSize.LARGE  -> 0.25f
        }

        val textSize = (minOf(tileWidth, tileHeight).value * when (config.size) {
            TileSize.SMALL  -> 0.12f
            TileSize.MEDIUM -> 0.12f
            TileSize.LARGE  -> 0.07f
            else            -> 0.08f
        }).sp

        Box(Modifier.fillMaxSize()) {

            val iconModifier = when (config.size) {
                TileSize.TINY -> Modifier.align(Alignment.Center)
                TileSize.SMALL -> Modifier.align(Alignment.Center)
                TileSize.MEDIUM -> Modifier.align(Alignment.Center)
                TileSize.LARGE -> Modifier.align(Alignment.Center)
            }

            SharedAppIcon(
                packageName = config.packageName,
                customIcon = customIcon,
                iconSize = iconSize,
                notificationCount = notificationCount,
                modifier = iconModifier
            )

            if (config.size != TileSize.TINY) {

                val horizontalPadding = tileWidth * 0.05f
                val bottomPadding = tileHeight * 0.06f

                Text(
                    text = config.title,
                    color = contentColor,
                    fontSize = textSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = horizontalPadding,
                            bottom = bottomPadding
                        )
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