package com.example.fluentui.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.R
import com.example.fluentui.data.SettingsManager
import kotlinx.coroutines.launch

val MicaBg = Color(0xFF000000).copy(alpha = 0.70f)
val MicaCard = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val WinText = Color(0xFFFFFFFF)
val WinSub = Color(0xFFD0D0D0)
val WinAccent = Color(0xFF60CDFF)

@Composable
fun SettingsContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val columns by settingsManager.columnCount.collectAsState(initial = 4)
    val rows by settingsManager.rowCount.collectAsState(initial = 6) // Récupération des lignes
    val iconSize by settingsManager.iconSize.collectAsState(initial = 60f)
    val showLabels by settingsManager.showLabels.collectAsState(initial = true)
    val isVertical by settingsManager.isVerticalScroll.collectAsState(initial = false)

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(colors = listOf(MicaBg, Color(0xFF000000).copy(alpha = 0.95f))),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.2f)))

        Text("Personnalisation", color = WinText, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp, top = 8.dp))

        FluentCard {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(WinAccent), contentAlignment = Alignment.Center) {
                    Icon(rememberVectorPainter(Icons.Default.Person), null, tint = Color.Black, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Utilisateur", color = WinText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text("Compte Local", color = WinSub, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("Affichage")

        FluentCard {
            Column(modifier = Modifier.padding(16.dp)) {
                // SÉLECTEUR COLONNES
                Text("Colonnes : $columns", color = WinText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4, 5).forEach { num ->
                        val isSelected = columns == num
                        Box(modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(4.dp)).background(if (isSelected) WinAccent else Color.White.copy(alpha = 0.05f)).clickable { scope.launch { settingsManager.setColumnCount(num) } }, contentAlignment = Alignment.Center) {
                            Text(num.toString(), color = if (isSelected) Color.Black else WinText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SÉLECTEUR LIGNES
                Text("Lignes : $rows", color = WinText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 6).forEach { num ->
                        val isSelected = rows == num
                        Box(modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(4.dp)).background(if (isSelected) WinAccent else Color.White.copy(alpha = 0.05f)).clickable { scope.launch { settingsManager.setRowCount(num) } }, contentAlignment = Alignment.Center) {
                            Text(num.toString(), color = if (isSelected) Color.Black else WinText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("Taille des icônes : ${iconSize.toInt()}dp", color = WinText, fontSize = 14.sp)
                Slider(value = iconSize, onValueChange = { scope.launch { settingsManager.setIconSize(it) } }, valueRange = 40f..80f, colors = SliderDefaults.colors(thumbColor = WinAccent, activeTrackColor = WinAccent, inactiveTrackColor = Color.White.copy(alpha = 0.1f)))

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Défilement Vertical", color = WinText)
                        Text(if(isVertical) "Liste de A à Z" else "Pages horizontales (One UI)", color = WinSub, fontSize = 12.sp)
                    }
                    Switch(checked = isVertical, onCheckedChange = { scope.launch { settingsManager.setVerticalScroll(it) } }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = WinAccent))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Afficher les noms", color = WinText, modifier = Modifier.weight(1f))
                    Switch(checked = showLabels, onCheckedChange = { scope.launch { settingsManager.setShowLabels(it) } }, colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = WinAccent))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle("Système")
        FluentCard {
            FluentItem(painterResource(id = R.drawable.ic_fluent_image_24_regular), "Arrière-plan", "Changer le fond d'écran") { context.startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) }
            FluentDivider()
            FluentItem(painterResource(id = R.drawable.ic_fluent_settings_24_regular), "Paramètres Android", "Ouvrir les réglages natifs") { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
        Spacer(modifier = Modifier.height(navBarPadding + 48.dp))
    }
}

@Composable fun FluentCard(content: @Composable () -> Unit) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MicaCard).border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(8.dp))) { content() } }
@Composable fun FluentItem(icon: Painter, title: String, subtitle: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp, 14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = WinText.copy(0.9f), modifier = Modifier.size(24.dp)); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(title, color = WinText, fontSize = 16.sp); Text(subtitle, color = WinSub, fontSize = 13.sp) }; Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = WinSub, modifier = Modifier.size(18.dp)) } }
@Composable fun FluentDivider() { HorizontalDivider(color = Color.White.copy(0.05f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp)) }
@Composable fun SectionTitle(text: String) { Text(text, color = WinText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)) }