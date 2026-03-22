package com.example.fluentui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun FluentBadgedBox(
    badgeCount: Int,
    modifier: Modifier = Modifier,
    badgeColor: Color = Color(0xFF0078D4),
    cutoutColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()

        if (badgeCount > 0) {
            val isDark = isSystemInDarkTheme()
            val defaultCutout = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)

            FluentBadge(
                count = badgeCount,
                badgeColor = badgeColor,
                strokeColor = cutoutColor ?: defaultCutout,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .zIndex(10f)
            )
        }
    }
}

@Composable
fun FluentBadge(
    count: Int,
    badgeColor: Color,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    val badgeText = if (count > 99) "99+" else count.toString()
    val textColor = if (badgeColor.luminance() > 0.5f) Color.Black else Color.White

    Box(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = CircleShape, spotColor = badgeColor.copy(alpha = 0.5f))
            .height(26.dp)
            .widthIn(min = 26.dp)
            .border(2.dp, strokeColor, CircleShape)
            .background(badgeColor, CircleShape)
            .padding(horizontal = if (count > 9) 8.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badgeText,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}