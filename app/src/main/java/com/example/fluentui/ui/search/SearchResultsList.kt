package com.example.fluentui.ui.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.components.SharedAppIcon
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.filled.WebAsset
import fluent.ui.system.icons.regular.*

/**
 * ==============================================================================
 * WINDOWS 11 SEARCH RESULTS LIST WITH RICH ACTIONS
 * ==============================================================================
 * Engineering Note: Implements the Fluent Design visual hierarchy.
 * Enhanced with Contextual Rich Actions (Call/SMS for contacts, Uninstall/Info for apps).
 * Uses Android Implicit Intents for seamless OS integration.
 */
@Composable
fun SearchResultsList(
    results: List<SearchResult>,
    isDark: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) return

    // Surface color for the cohesive cards (Win11 style)
    val surfaceColor = remember(isDark) { if (isDark) Color(0xFF252525) else Color(0xFFF9F9F9) }
    val textColor = remember(isDark) { if (isDark) Color.White else Color.Black }

    // Logic: Separate the Top Match from the rest, then group the rest by Type
    val bestMatch = results.first()
    val otherResultsGrouped = remember(results) {
        results.drop(1).groupBy { it.type }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // --- SECTION 1: MEILLEUR RÉSULTAT (BEST MATCH) ---
        item {
            SectionHeader("Meilleur résultat", textColor)
            Spacer(modifier = Modifier.height(8.dp))
            BestMatchCard(
                result = bestMatch,
                surfaceColor = surfaceColor,
                textColor = textColor,
                accentColor = accentColor
            )
        }

        // --- SECTION 2: AUTRES CATÉGORIES GROUPÉES ---
        otherResultsGrouped.forEach { (type, categoryResults) ->
            item {
                SectionHeader(getCategoryName(type), textColor)
                Spacer(modifier = Modifier.height(8.dp))

                // Grouping items in a single cohesive card container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceColor)
                ) {
                    categoryResults.forEachIndexed { index, result ->
                        StandardResultItem(
                            result = result,
                            textColor = textColor,
                            accentColor = accentColor
                        )
                        // Add a subtle divider between items, except the last one
                        if (index < categoryResults.size - 1) {
                            Divider(
                                color = textColor.copy(alpha = 0.05f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// UI COMPONENTS (BEST MATCH & STANDARD ITEM)
// ==============================================================================

/**
 * Windows 11 "Best Match" Card.
 * Features larger icon, prominent typography, and context actions.
 */
@Composable
private fun BestMatchCard(
    result: SearchResult,
    surfaceColor: Color,
    textColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .clickable { handleResultClick(context, result) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large Icon Layout (48dp)
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (result.type == ResultType.APP) {
                SharedAppIcon(
                    packageName = result.intentData as String,
                    size = 48f,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(
                    imageVector = getIconForResult(result),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Prominent Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                fontSize = 17.sp,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = result.subtitle,
                fontSize = 13.sp,
                color = textColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // --- NEW: RICH ACTIONS (Appelez, SMS, Désinstaller) ---
        ResultRichActions(result = result, textColor = textColor, accentColor = accentColor)
    }
}

/**
 * Standard list item designed to be placed inside a cohesive group container.
 */
@Composable
private fun StandardResultItem(
    result: SearchResult,
    textColor: Color,
    accentColor: Color
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { handleResultClick(context, result) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Standard Icon Layout (32dp)
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (result.type == ResultType.APP) {
                SharedAppIcon(
                    packageName = result.intentData as String,
                    size = 32f,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = getIconForResult(result),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Standard Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                fontSize = 15.sp,
                color = textColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = result.subtitle,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // --- NEW: RICH ACTIONS ---
        ResultRichActions(result = result, textColor = textColor, accentColor = accentColor)
    }
}

/**
 * Displays contextual actions based on the ResultType (Call, SMS, Uninstall, Info).
 * Engineering Note: Uses specific Android Intents to delegate actions to the OS.
 */
@Composable
private fun ResultRichActions(result: SearchResult, textColor: Color, accentColor: Color) {
    val context = LocalContext.current

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        when (result.type) {
            ResultType.APP -> {
                val packageName = result.intentData as String

                // Action: App Info
                ActionIconButton(
                    icon = FluentIcons.Regular.Info,
                    tint = textColor.copy(alpha = 0.6f),
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        context.startActivity(intent)
                    }
                )

                // Action: Uninstall
                ActionIconButton(
                    icon = FluentIcons.Regular.Delete,
                    tint = Color(0xFFFF5555), // Red warning color
                    onClick = {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            ResultType.CONTACT -> {
                val phoneNumber = result.subtitle // Assumes subtitle contains the phone number

                // Action: Call
                ActionIconButton(
                    icon = FluentIcons.Regular.Call,
                    tint = accentColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        context.startActivity(intent)
                    }
                )

                // Action: SMS
                ActionIconButton(
                    icon = FluentIcons.Regular.Chat,
                    tint = accentColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$phoneNumber")
                        }
                        context.startActivity(intent)
                    }
                )
            }
            else -> {
                // No secondary actions for other types yet
            }
        }
    }
}

/**
 * Compact icon button specifically designed for result list actions.
 */
@Composable
private fun ActionIconButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, textColor: Color) {
    Text(
        text = title,
        color = textColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp)
    )
}

// ==============================================================================
// HELPER FUNCTIONS
// ==============================================================================

private fun getCategoryName(type: ResultType): String {
    return when (type) {
        ResultType.APP -> "Applications"
        ResultType.FILE -> "Documents"
        ResultType.SETTING -> "Paramètres système"
        ResultType.WEB -> "Recherche Web"
        ResultType.CONTACT -> "Contacts"
        ResultType.CALCULATION -> "Calculatrice"
        ResultType.CONVERSION -> "Conversion"
        ResultType.ACTION -> "Actions rapides"
    }
}

private fun getIconForResult(result: SearchResult): ImageVector {
    return when (result.type) {
        ResultType.FILE -> getFileIconVector(result.title)
        ResultType.CONTACT -> FluentIcons.Regular.People
        ResultType.SETTING -> FluentIcons.Regular.Settings
        ResultType.WEB -> FluentIcons.Filled.WebAsset
        ResultType.CALCULATION -> FluentIcons.Regular.Calculator
        ResultType.CONVERSION -> FluentIcons.Regular.ClipboardMathFormula
        ResultType.ACTION -> FluentIcons.Regular.AppGeneric
        ResultType.APP -> FluentIcons.Regular.Apps
    }
}

private fun getFileIconVector(fileName: String): ImageVector {
    val name = fileName.lowercase()
    return when {
        name.endsWith(".pdf") -> FluentIcons.Regular.DocumentPdf
        name.endsWith(".docx") || name.endsWith(".doc") -> FluentIcons.Regular.DocumentText
        name.endsWith(".xlsx") || name.endsWith(".xls") -> FluentIcons.Regular.DocumentTable
        name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") -> FluentIcons.Regular.Image
        name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") -> FluentIcons.Regular.Video
        name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac") -> FluentIcons.Regular.MusicNote2
        name.endsWith(".zip") || name.endsWith(".rar") -> FluentIcons.Regular.FolderZip
        else -> FluentIcons.Regular.Document
    }
}

private fun handleResultClick(context: Context, result: SearchResult) {
    try {
        when (result.type) {
            ResultType.APP -> {
                context.packageManager
                    .getLaunchIntentForPackage(result.intentData as String)
                    ?.let { context.startActivity(it) }
            }

            ResultType.FILE -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = result.intentData as Uri
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }

            ResultType.WEB -> {
                val url = if (result.intentData.toString().startsWith("http")) {
                    result.intentData.toString()
                } else {
                    "https://www.google.com/search?q=${Uri.encode(result.intentData.toString())}"
                }
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }

            else -> { /* Other types handled by relevant strategies */ }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Impossible d'ouvrir : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}