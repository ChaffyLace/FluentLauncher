package com.example.fluentui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.R

@Composable
fun EditModeMenu(
    onWallpaperClick: () -> Unit,
    onWidgetsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF191919).copy(alpha = 0.9f))
            .navigationBarsPadding()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditModeItem(
            icon = painterResource(id = R.drawable.ic_fluent_image_24_regular),
            label = "Fond d'écran",
            onClick = onWallpaperClick
        )

        EditModeItem(
            icon = painterResource(id = R.drawable.ic_fluent_apps_24_regular),
            label = "Widgets",
            onClick = onWidgetsClick
        )

        EditModeItem(
            icon = painterResource(id = R.drawable.ic_fluent_settings_24_regular),
            label = "Paramètres",
            onClick = onSettingsClick
        )
    }
}

@Composable
fun EditModeItem(icon: Painter, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2D2D2D)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = icon, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}