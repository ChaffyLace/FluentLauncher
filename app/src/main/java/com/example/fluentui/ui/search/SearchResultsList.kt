package com.example.fluentui.ui.search

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.filled.WebAsset
import fluent.ui.system.icons.regular.*

// ==============================================================================
// SEARCH RESULTS LIST
// ==============================================================================

@Composable
fun SearchResultsList(
    results: List<SearchResult>,
    isDark: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(results) { result ->
            SearchResultItem(
                result = result,
                isDark = isDark,
                accentColor = accentColor
            )
        }
    }
}

// ==============================================================================
// SEARCH RESULT ITEM
// ==============================================================================

@Composable
private fun SearchResultItem(
    result: SearchResult,
    isDark: Boolean,
    accentColor: Color
) {
    val context = LocalContext.current
    val bgColor = if (isDark) Color(0xFF202020) else Color(0xFFF0F0F0)
    val textColor = if (isDark) Color.White else Color.Black

    val onClick = {
        handleResultClick(context, result)
    }

    val iconVector = when (result.type) {
        ResultType.CONTACT -> FluentIcons.Regular.People
        ResultType.APP -> FluentIcons.Regular.Apps
        ResultType.SETTING -> FluentIcons.Regular.Settings
        ResultType.FILE -> FluentIcons.Regular.Folder
        ResultType.WEB -> FluentIcons.Filled.WebAsset
        ResultType.CALCULATION -> FluentIcons.Regular.Calculator
        ResultType.CONVERSION -> FluentIcons.Regular.ClipboardMathFormula
        ResultType.ACTION -> FluentIcons.Regular.AppGeneric
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        if (result.type == ResultType.APP && result.iconData is Bitmap) {
            Image(
                bitmap = (result.iconData as Bitmap).asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Text(
                text = result.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.6f)
            )
        }
    }
}

// ==============================================================================
// RESULT CLICK HANDLER
// ==============================================================================

private fun handleResultClick(context: Context, result: SearchResult) {
    try {
        when (result.type) {
            ResultType.APP -> {
                val intent = context.packageManager
                    .getLaunchIntentForPackage(result.intentData as String)
                if (intent != null) {
                    context.startActivity(intent)
                }
            }

            ResultType.CONTACT -> {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.withAppendedPath(
                        android.provider.ContactsContract.Contacts.CONTENT_URI,
                        result.intentData as String
                    )
                )
                context.startActivity(intent)
            }

            ResultType.SETTING, ResultType.ACTION -> {
                context.startActivity(result.intentData as Intent)
            }

            ResultType.FILE -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = result.intentData as Uri
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(intent)
            }

            ResultType.WEB -> {
                val url = if (result.intentData.toString().startsWith("http")) {
                    result.intentData.toString()
                } else {
                    "https://www.google.com/search?q=${Uri.encode(result.intentData.toString())}"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }

            ResultType.CALCULATION, ResultType.CONVERSION -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                val clip = android.content.ClipData.newPlainText(
                    "Result",
                    result.intentData.toString()
                )
                clipboard.setPrimaryClip(clip)
                Toast.makeText(
                    context,
                    "Copié : ${result.intentData}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(
            context,
            "Erreur : ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}