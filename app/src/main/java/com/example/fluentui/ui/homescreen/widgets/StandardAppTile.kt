package com.example.fluentui.ui.homescreen.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.components.InternalAppIcon
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize

import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.color.*

@Composable
fun StandardAppTile(config: TileConfig, contentColor: Color) {
    val horizontalPadding = when (config.size) {
        TileSize.TINY -> 4.dp
        TileSize.SMALL -> 8.dp
        TileSize.MEDIUM -> 10.dp
        TileSize.LARGE -> 12.dp
        else -> 8.dp
    }

    val bottomPadding = when (config.size) {
        TileSize.TINY -> 4.dp
        TileSize.SMALL -> 6.dp
        TileSize.MEDIUM -> 8.dp
        TileSize.LARGE -> 10.dp
        else -> 6.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val iconOffset = when (config.size) {
            TileSize.SMALL -> (-6).dp
            TileSize.MEDIUM -> (-4).dp
            TileSize.LARGE -> (-8).dp
            else -> 0.dp
        }

        val iconSize = when (config.size) {
            TileSize.TINY -> 28.dp
            TileSize.SMALL -> 40.dp
            TileSize.MEDIUM -> 48.dp
            TileSize.LARGE -> 64.dp
            else -> 40.dp
        }

        SharedAppIcon(
            packageName = config.packageName,
            size = iconSize.value * 2.5f,
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.Center)
                .offset(y = iconOffset)
        )

        if (config.size != TileSize.TINY) {
            Text(
                text = config.title,
                fontSize = when (config.size) {
                    TileSize.SMALL -> 11.sp
                    TileSize.MEDIUM -> 12.sp
                    TileSize.LARGE -> 13.sp
                    else -> 11.sp
                },
                color = contentColor,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = horizontalPadding, end = horizontalPadding, bottom = bottomPadding)
                    .fillMaxWidth(),
                maxLines = if (config.size == TileSize.LARGE) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SharedAppIcon(packageName: String, size: Float, modifier: Modifier) {
    val customIcon = getCustomFluentIcon(packageName)
    if (customIcon != null) {
        Icon(
            imageVector = customIcon,
            contentDescription = null,
            modifier = modifier,
            tint = Color.Unspecified
        )
    } else {
        InternalAppIcon(packageName, size, modifier)
    }
}

private fun getCustomFluentIcon(packageName: String): ImageVector? {
    val pkg = packageName.lowercase()
    return when {
        pkg.contains("calendar") -> FluentIcons.Color.Calendar
        pkg.contains("camera") -> FluentIcons.Color.Camera
        pkg.contains("messaging") || pkg.contains("whatsapp") -> FluentIcons.Color.Chat
        pkg.contains("clock") || pkg.contains("alarm") -> FluentIcons.Color.Clock
        pkg.contains("gallery") || pkg.contains("photos") -> FluentIcons.Color.Image
        pkg.contains("maps") -> FluentIcons.Color.LocationRipple
        pkg.contains("mail") || pkg.contains("gmail") -> FluentIcons.Color.Mail
        pkg.contains("contacts") -> FluentIcons.Color.Person
        pkg.contains("browser") || pkg.contains("chrome") -> FluentIcons.Color.Planet
        pkg.contains("settings") -> FluentIcons.Color.Settings
        pkg.contains("video") -> FluentIcons.Color.Video
        pkg.contains("note") -> FluentIcons.Color.Notebook
        pkg.contains("file") || pkg.contains("explorer") -> FluentIcons.Color.DocumentFolder
        else -> null
    }
}