package com.example.fluentui.ui.search

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.components.AppModel
import com.example.fluentui.ui.components.SharedAppIcon
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.*

/**
 * ==============================================================================
 * DYNAMIC ZERO-STATE DASHBOARD
 * ==============================================================================
 * Engineering Note: UI is strictly driven by real telemetry data.
 * Top Apps reflect actual launch counts. Recent Searches reflect actual history.
 * Quick Actions have been updated to high-utility Android settings.
 */
@Composable
fun SearchDashboard(
    topApps: List<AppModel>, // INJECTED: Real telemetry data
    recentSearches: List<String>, // INJECTED: Real search history
    onAppClick: (String) -> Unit,
    onRecentSearchClick: (String) -> Unit,
    isDark: Boolean,
    accentColor: Color
) {
    val context = LocalContext.current

    val surfaceColor = remember(isDark) { if (isDark) Color(0xFF252525) else Color(0xFFF9F9F9) }
    val textColor = remember(isDark) { if (isDark) Color.White else Color.Black }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // --- SECTION 1: TOP APPLICATIONS (REAL USAGE) ---
        if (topApps.isNotEmpty()) {
            item {
                SectionTitle("Fréquemment utilisées", textColor)
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        topApps.take(4).forEach { app ->
                            TopAppGridItem(
                                app = app,
                                textColor = textColor,
                                modifier = Modifier.weight(1f),
                                onClick = { onAppClick(app.packageName) }
                            )
                        }
                    }
                    if (topApps.size > 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            topApps.drop(4).take(4).forEach { app ->
                                TopAppGridItem(
                                    app = app,
                                    textColor = textColor,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onAppClick(app.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 2: RECHERCHES RÉCENTES (REAL HISTORY) ---
        if (recentSearches.isNotEmpty()) {
            item {
                SectionTitle("Recherches récentes", textColor)
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(
                            maxWidth = constraints.maxWidth + 32.dp.roundToPx()
                        ))
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
                ) {
                    item { Spacer(modifier = Modifier.width(16.dp)) }

                    items(recentSearches) { search ->
                        RecentSearchChip(
                            text = search,
                            surfaceColor = surfaceColor,
                            textColor = textColor,
                            onClick = { onRecentSearchClick(search) }
                        )
                    }

                    item { Spacer(modifier = Modifier.width(16.dp)) }
                }
            }
        }

        // --- SECTION 3: ACTIONS RAPIDES (POWER USER OPTIONS) ---
        item {
            SectionTitle("Paramètres rapides", textColor)
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaceColor)
            ) {
                QuickActionItem(
                    title = "Paramètres d'affichage",
                    icon = FluentIcons.Regular.CenterVertical,
                    textColor = textColor,
                    accentColor = accentColor,
                    onClick = { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
                )

                QuickActionDivider(textColor)

                QuickActionItem(
                    title = "Gérer les applications",
                    icon = FluentIcons.Regular.AppsList,
                    textColor = textColor,
                    accentColor = accentColor,
                    onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS)) }
                )

                QuickActionDivider(textColor)

                QuickActionItem(
                    title = "Stockage interne",
                    icon = FluentIcons.Regular.FolderZip,
                    textColor = textColor,
                    accentColor = accentColor,
                    onClick = { context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) }
                )
            }
        }
    }
}

// UI Components (Kept Identical to previous excellent layout)
@Composable
private fun SectionTitle(title: String, textColor: Color) {
    Text(text = title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun TopAppGridItem(app: AppModel, textColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SharedAppIcon(packageName = app.packageName, size = 48f, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = app.label, color = textColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RecentSearchChip(text: String, surfaceColor: Color, textColor: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.clip(CircleShape).background(surfaceColor).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = FluentIcons.Regular.History, contentDescription = null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QuickActionItem(title: String, icon: ImageVector, textColor: Color, accentColor: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(imageVector = FluentIcons.Regular.ChevronRight, contentDescription = null, tint = textColor.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun QuickActionDivider(textColor: Color) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(textColor.copy(alpha = 0.05f)))
}