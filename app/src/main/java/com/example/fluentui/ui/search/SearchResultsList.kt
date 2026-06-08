/**
 * @file SearchResultsList.kt
 * @description Windows 11 Search Results implementation.
 * Features: Best Match hierarchy, grouped categories, and contextual rich actions.
 * Engineering Note: Fully localized and accessibility-ready.
 */

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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.R
import com.example.fluentui.ui.components.SharedAppIcon
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.filled.WebAsset
import fluent.ui.system.icons.regular.*

@Composable
fun SearchResultsList(
    results: List<SearchResult>,
    isDark: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) return

    val surfaceColor = remember(isDark) { if (isDark) Color(0xFF252525) else Color(0xFFF9F9F9) }
    val textColor = remember(isDark) { if (isDark) Color.White else Color.Black }

    val bestMatch = results.first()
    val otherResultsGrouped = remember(results) {
        results.drop(1).groupBy { it.type }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // --- SECTION 1: BEST MATCH ---
        item {
            SectionHeader(stringResource(R.string.search_best_match), textColor)
            Spacer(modifier = Modifier.height(8.dp))
            BestMatchCard(bestMatch, surfaceColor, textColor, accentColor)
        }

        // --- SECTION 2: GROUPED CATEGORIES ---
        otherResultsGrouped.forEach { (type, categoryResults) ->
            item {
                SectionHeader(stringResource(getCategoryNameRes(type)), textColor)
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(surfaceColor)
                ) {
                    categoryResults.forEachIndexed { index, result ->
                        StandardResultItem(result, textColor, accentColor)
                        if (index < categoryResults.size - 1) {
                            HorizontalDivider(
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
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (result.type == ResultType.APP) {
                SharedAppIcon(packageName = result.intentData as String, size = 48f, modifier = Modifier.size(48.dp))
            } else {
                Icon(imageVector = getIconForResult(result), contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = result.title, fontSize = 17.sp, color = textColor, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            result.subtitle?.let {
                Text(text = it, fontSize = 13.sp, color = textColor.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        ResultRichActions(result = result, textColor = textColor, accentColor = accentColor)
    }
}

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
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            if (result.type == ResultType.APP) {
                SharedAppIcon(packageName = result.intentData as String, size = 32f, modifier = Modifier.size(32.dp))
            } else {
                Icon(imageVector = getIconForResult(result), contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = result.title, fontSize = 15.sp, color = textColor, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            result.subtitle?.let {
                Text(text = it, fontSize = 12.sp, color = textColor.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        ResultRichActions(result = result, textColor = textColor, accentColor = accentColor)
    }
}

@Composable
private fun ResultRichActions(result: SearchResult, textColor: Color, accentColor: Color) {
    val context = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        when (result.type) {
            ResultType.APP -> {
                val pkg = result.intentData as String
                ActionIconButton(
                    icon = FluentIcons.Regular.Info,
                    descRes = R.string.desc_app_info,
                    tint = textColor.copy(alpha = 0.6f),
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:$pkg") }
                        context.startActivity(intent)
                    }
                )
                ActionIconButton(
                    icon = FluentIcons.Regular.Delete,
                    descRes = R.string.desc_app_uninstall,
                    tint = Color(0xFFFF5555),
                    onClick = {
                        val intent = Intent(Intent.ACTION_DELETE).apply { data = Uri.parse("package:$pkg") }
                        context.startActivity(intent)
                    }
                )
            }
            ResultType.CONTACT -> {
                val phone = result.subtitle ?: ""
                ActionIconButton(
                    icon = FluentIcons.Regular.Call,
                    descRes = R.string.desc_contact_call,
                    tint = accentColor,
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun ActionIconButton(icon: ImageVector, descRes: Int, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = stringResource(descRes), tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SectionHeader(title: String, textColor: Color) {
    Text(text = title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
}

// --- HELPERS ---

private fun getCategoryNameRes(type: ResultType): Int = when (type) {
    ResultType.APP -> R.string.search_cat_apps
    ResultType.FILE -> R.string.search_cat_files
    ResultType.SETTING -> R.string.search_cat_settings
    ResultType.WEB -> R.string.search_cat_web
    ResultType.CONTACT -> R.string.search_cat_contacts
    ResultType.CALCULATION -> R.string.search_cat_calc
    ResultType.CONVERSION -> R.string.search_cat_conv
    ResultType.ACTION -> R.string.search_cat_actions
}

private fun getIconForResult(result: SearchResult): ImageVector = when (result.type) {
    ResultType.FILE -> getFileIconVector(result.title)
    ResultType.CONTACT -> FluentIcons.Regular.People
    ResultType.SETTING -> FluentIcons.Regular.Settings
    ResultType.WEB -> FluentIcons.Filled.WebAsset
    ResultType.CALCULATION -> FluentIcons.Regular.Calculator
    ResultType.CONVERSION -> FluentIcons.Regular.ClipboardMathFormula
    ResultType.ACTION -> FluentIcons.Regular.AppGeneric
    ResultType.APP -> FluentIcons.Regular.Apps
}

private fun getFileIconVector(fileName: String): ImageVector {
    val name = fileName.lowercase()
    return when {
        name.endsWith(".pdf") -> FluentIcons.Regular.DocumentPdf
        name.endsWith(".jpg") || name.endsWith(".png") -> FluentIcons.Regular.Image
        name.endsWith(".mp4") -> FluentIcons.Regular.Video
        name.endsWith(".mp3") -> FluentIcons.Regular.MusicNote2
        name.endsWith(".zip") -> FluentIcons.Regular.FolderZip
        else -> FluentIcons.Regular.Document
    }
}

private fun handleResultClick(context: Context, result: SearchResult) {
    try {
        when (result.type) {
            ResultType.APP -> {
                context.packageManager.getLaunchIntentForPackage(result.intentData as String)
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
                val url = result.intentData.toString().let {
                    if (it.startsWith("http")) it else "https://www.google.com/search?q=${Uri.encode(it)}"
                }
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            else -> {}
        }
    } catch (e: Exception) {
        val errorMsg = context.getString(R.string.search_error_open, e.localizedMessage ?: "")
        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
    }
}