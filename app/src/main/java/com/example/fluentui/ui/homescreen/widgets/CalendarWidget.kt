package com.example.fluentui.ui.homescreen.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.color.Calendar

@Composable
fun CalendarWidget(config: TileConfig, contentColor: Color) {
    val padding = if (config.size == TileSize.SMALL) 12.dp else 16.dp

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Icon(
            imageVector = FluentIcons.Color.Calendar,
            contentDescription = null,
            modifier = Modifier.size(if (config.size == TileSize.SMALL) 24.dp else 32.dp).align(Alignment.TopStart),
            tint = Color.Unspecified
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Aujourd'hui",
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "14:00 - Réunion",
                color = contentColor,
                fontSize = if (config.size == TileSize.SMALL) 14.sp else 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (config.size == TileSize.LARGE) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}