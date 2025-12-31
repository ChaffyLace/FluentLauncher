package com.example.fluentui.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FluentSearchDock(
    modifier: Modifier = Modifier,
    placeholder: String = "Type here to search"
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val dockColor = if (isDark) {
        Color(0xFF151515).copy(alpha = 0.70f)
    } else {
        Color(0xFFF3F3F3).copy(alpha = 0.70f)
    }

    val inputColor = if (isDark) {
        Color(0xFF000000).copy(alpha = 0.50f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.80f)
    }

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.20f),
            Color.Transparent
        )
    )

    val contentColor = if (isDark) Color(0xFFCCCCCC) else Color(0xFF555555)

    Box(
        modifier = modifier
            .width(340.dp)
            .height(84.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(dockColor),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(inputColor)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_WEB_SEARCH)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = placeholder,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}