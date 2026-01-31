package com.example.fluentui.ui.homescreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.components.InternalAppIcon
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.ui.system.TileType
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.fluentMicaComplex // <--- L'IMPORT ESSENTIEL

@Composable
fun MicaTile(
    config: TileConfig,
    mica: MicaParams?,
    isDark: Boolean,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    isTransparencyEnabled: Boolean,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val cornerRadius = if (config.size == TileSize.TINY) 8.dp else 12.dp
    val tileShape = RoundedCornerShape(cornerRadius)

    // Animation de pression (Scale)
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "press_scale",
        animationSpec = tween(100)
    )

    val isRichMedia = isRichMediaApp(config.packageName)

    // Pour les Apps système standard, on peut ajouter une petite bordure spéculaire en plus du Mica
    val isSystemApp = config.type == TileType.APPLICATION && !isRichMedia

    Box(
        modifier = modifier
            .scale(scale)
            // --- C'EST ICI LE CHANGEMENT MAJEUR ---
            // Au lieu de calculer des Brush compliqués, on appelle ton moteur graphique.
            // Il gère : L'ombre, le Clip, le Fond Gris (si désactivé) OU le Fond Flou (si activé).
            .fluentMicaComplex(
                micaParams = mica,
                enabled = isTransparencyEnabled,
                elevation = if (isDark) 6.dp else 4.dp, // L'ombre est gérée dedans
                shape = tileShape
            )
            // Gestion du clic
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = {
                        if (config.packageName.isNotEmpty()) {
                            runCatching {
                                val intent = context.packageManager.getLaunchIntentForPackage(config.packageName)
                                if (intent != null) context.startActivity(intent)
                            }
                        }
                    }
                )
            }
    ) {
        // LAYER 1: FINITIONS SUPPLÉMENTAIRES (Optionnel)
        if (isSystemApp && isTransparencyEnabled) {
            // Petite bordure "lumière" en haut (Specular) pour faire encore plus 3D
            val borderBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(if (isDark) 0.15f else 0.4f),
                    Color.Transparent
                )
            )
            Box(Modifier.matchParentSize().border(1.dp, borderBrush, tileShape))
        }

        // LAYER 2: LE CONTENU DE LA TUILE
        Box(modifier = Modifier.fillMaxSize()) {
            when (config.type) {
                TileType.CALENDAR -> CalendarWidget(config)
                TileType.MAIL -> MailWidget(config)
                TileType.CLOCK -> ClockWidget(config)
                TileType.BATTERY -> BatteryWidget(config)
                TileType.WEATHER -> WeatherWidget(config)
                else -> {
                    if (isRichMedia) {
                        RichMediaTileContent(config)
                    } else {
                        // STANDARD APP CONTENT
                        Box(Modifier.fillMaxSize().padding(12.dp)) {
                            val iconSize = when (config.size) {
                                TileSize.TINY -> 28.dp
                                TileSize.SMALL -> 36.dp
                                else -> 52.dp
                            }

                            SharedAppIcon(
                                packageName = config.packageName,
                                size = when (config.size) {
                                    TileSize.TINY -> 48f; TileSize.SMALL -> 64f; else -> 96f
                                },
                                modifier = Modifier.size(iconSize).align(Alignment.Center)
                            )

                            if (config.size != TileSize.TINY) {
                                Text(
                                    text = config.title,
                                    fontSize = if (config.size == TileSize.SMALL) 11.sp else 12.sp,
                                    color = if (isDark) Color.White.copy(0.9f) else Color.Black.copy(0.85f),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.BottomStart),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- LES FONCTIONS UTILITAIRES ET WIDGETS ---

@Composable
fun SharedAppIcon(packageName: String, size: Float, modifier: Modifier) {
    InternalAppIcon(packageName, size, modifier)
}

fun isRichMediaApp(packageName: String): Boolean {
    val pkg = packageName.lowercase()
    return pkg.contains("netflix") || pkg.contains("spotify") ||
            pkg.contains("tiktok") || pkg.contains("youtube") ||
            pkg.contains("disney")
}

@Composable
fun RichMediaTileContent(config: TileConfig) {
    val pkg = config.packageName.lowercase()
    // Note: Pour les apps média, on garde souvent leur couleur de marque opaque
    // car ça fait partie de leur identité, mais tu peux enlever le .background() si tu veux du Mica ici aussi.
    Box(Modifier.fillMaxSize()) {
        when {
            pkg.contains("netflix") -> {
                Box(Modifier.fillMaxSize().background(Color.Black))
                Text("N", color = Color(0xFFE50914), fontWeight = FontWeight.Bold, fontSize = 56.sp, modifier = Modifier.align(Alignment.Center))
            }
            pkg.contains("spotify") -> {
                Box(Modifier.fillMaxSize().background(Color(0xFF1DB954)))
                Box(Modifier.size(32.dp).background(Color.Black, CircleShape).align(Alignment.Center))
            }
            pkg.contains("youtube") -> {
                Box(Modifier.fillMaxSize().background(Color(0xFFFF0000)))
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp).align(Alignment.Center))
            }
            else -> {
                Box(Modifier.fillMaxSize().background(Color(0xFF333333)))
                Text(config.title.take(1).uppercase(), color = Color.White, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// --- WIDGETS ---
// J'ai retiré les fonds opaques des widgets pour qu'on voit le Mica derrière !

@Composable
fun CalendarWidget(config: TileConfig) {
    // On passe transparent = true pour profiter du Mica de la tuile
    FluentWidgetLayout(isDark = isSystemInDarkTheme()) {
        Icon(Icons.Default.DateRange, null, tint = Color(0xFF60CDFF), modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text("Lun 24", color = it, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("14:00 Réunion", color = it.copy(0.7f), fontSize = 12.sp)
    }
}

@Composable
fun MailWidget(config: TileConfig) {
    FluentWidgetLayout(isDark = isSystemInDarkTheme()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Email, null, tint = Color(0xFF0078D4), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Outlook", color = it.copy(0.6f), fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Text("Projet Alpha", color = it, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text("Re: Mise à jour...", color = it.copy(0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ClockWidget(config: TileConfig) {
    FluentWidgetLayout(isDark = isSystemInDarkTheme()) {
        Text("Paris", color = it.copy(0.6f), fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text("14:42", color = it, fontWeight = FontWeight.Light, fontSize = 32.sp)
        Text("Lun, 24 Oct", color = it.copy(0.8f), fontSize = 12.sp)
    }
}

@Composable
fun BatteryWidget(config: TileConfig) {
    FluentWidgetLayout(isDark = isSystemInDarkTheme()) {
        Icon(Icons.Default.Info, null, tint = Color(0xFF107C10), modifier = Modifier.size(22.dp))
        Spacer(Modifier.weight(1f))
        Text("85%", color = it, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("~ 5h 30min", color = it.copy(0.7f), fontSize = 11.sp)
    }
}

@Composable
fun WeatherWidget(config: TileConfig) {
    // La météo garde son dégradé spécifique car c'est joli, mais tu peux le retirer pour du Mica pur
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2F80ED), Color(0xFF56CCF2))))) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Text("21°", color = Color.White, fontWeight = FontWeight.Light, fontSize = 28.sp)
            Text("Ensoleillé", color = Color.White.copy(0.9f), fontSize = 12.sp)
        }
    }
}

@Composable
fun FluentWidgetLayout(isDark: Boolean, content: @Composable ColumnScope.(Color) -> Unit) {
    val textColor = if (isDark) Color.White else Color.Black
    // J'ai mis le fond à Transparent pour que le layout ne cache pas l'effet Mica de la tuile
    val bg = Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(12.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        content(textColor)
    }
}