package com.example.fluentui.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.components.AppModel
import com.example.fluentui.ui.components.SharedAppIcon // <--- IMPORT IMPORTANT

// ==============================================================================
// SEARCH DASHBOARD - SHOWN WHEN QUERY IS EMPTY
// ==============================================================================

@Composable
fun SearchDashboard(
    allApps: List<AppModel>,
    onAppClick: (String) -> Unit,
    isDark: Boolean
) {
    val recentApps = remember(allApps) { allApps.take(8) }
    val textColor = if (isDark) Color.White else Color.Black
    val cardColor = if (isDark) Color(0xFF202020) else Color(0xFFF0F0F0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(top = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section: Recent Apps
        Text(
            text = "Apps récentes",
            color = textColor.copy(0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(recentApps) { app ->
                RecentAppItem(
                    app = app,
                    cardColor = cardColor,
                    textColor = textColor,
                    onClick = { onAppClick(app.packageName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Suggestions
        Text(
            text = "Suggestions",
            color = textColor.copy(0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp)
        )

        SuggestionChip(
            text = "Ouvrir les paramètres",
            cardColor = cardColor,
            textColor = textColor,
            onClick = { /* TODO: Add Intent to Settings */ }
        )

        SuggestionChip(
            text = "Rechercher des fichiers",
            cardColor = cardColor,
            textColor = textColor,
            onClick = { /* TODO: Add Intent to Files */ }
        )
    }
}

// ==============================================================================
// RECENT APP ITEM
// ==============================================================================

@Composable
private fun RecentAppItem(
    app: AppModel,
    cardColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CORRECTION: Replaced direct Bitmap usage with SharedAppIcon
        // This handles async loading via package name
        SharedAppIcon(
            packageName = app.packageName,
            size = 48f,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = app.label,
            color = textColor,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==============================================================================
// SUGGESTION CHIP
// ==============================================================================

@Composable
private fun SuggestionChip(
    text: String,
    cardColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )
    }
}