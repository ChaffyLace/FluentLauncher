package com.example.fluentui.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.fluentMicaComplex
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Folder

// =============================================================================
// FOLDER ICON — Affiché dans la grille comme une app normale
// =============================================================================

@Composable
fun FolderIconItem(
    folder: AppFolder,
    iconSize: Float,
    fontSize: Float,
    showLabel: Boolean,
    accentColor: Color,
    micaParams: MicaParams,
    isTransparencyEnabled: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "folder_press"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try { tryAwaitRelease() } finally { isPressed = false }
                    },
                    onTap = { onClick() },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
            }
    ) {
        // ── Icône dossier : grille 2×2 de miniatures ────────────────────────
        Box(
            modifier = Modifier
                .size(iconSize.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(
                    if (isTransparencyEnabled)
                        Modifier.fluentMicaComplex(
                            micaParams = micaParams,
                            enabled = true,
                            elevation = 0.dp,
                            shape = RoundedCornerShape(14.dp)
                        )
                    else
                        Modifier.background(
                            if (isDark) Color(0xFF2D2D2D) else Color(0xFFE8E8E8),
                            RoundedCornerShape(14.dp)
                        )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Fond légèrement teinté
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.15f),
                                accentColor.copy(alpha = 0.05f)
                            )
                        )
                    )
            )

            if (folder.apps.size >= 4) {
                // 4 miniatures d'icônes dans le dossier
                val previewSize = iconSize * 0.38f
                val gap = iconSize * 0.04f
                Column(
                    modifier = Modifier.padding((iconSize * 0.12f).dp),
                    verticalArrangement = Arrangement.spacedBy(gap.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
                        InternalAppIcon(
                            folder.apps[0].packageName,
                            Modifier.size(previewSize.dp).clip(RoundedCornerShape(4.dp))
                        )
                        InternalAppIcon(
                            folder.apps[1].packageName,
                            Modifier.size(previewSize.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap.dp)) {
                        InternalAppIcon(
                            folder.apps[2].packageName,
                            Modifier.size(previewSize.dp).clip(RoundedCornerShape(4.dp))
                        )
                        InternalAppIcon(
                            folder.apps[3].packageName,
                            Modifier.size(previewSize.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            } else if (folder.apps.isNotEmpty()) {
                // Moins de 4 apps : icône centrale
                val previewSize = iconSize * 0.55f
                InternalAppIcon(
                    folder.apps[0].packageName,
                    Modifier.size(previewSize.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                // Dossier vide
                Icon(
                    imageVector = FluentIcons.Regular.Folder,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size((iconSize * 0.5f).dp)
                )
            }
        }

        if (showLabel) {
            Text(
                text = folder.name,
                fontSize = fontSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = micaParams.primaryColor,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width((iconSize + 24).dp)
            )
        }
    }
}

// =============================================================================
// FOLDER OVERLAY — S'ouvre au tap sur le dossier
// =============================================================================

@Composable
fun FolderOverlay(
    folder: AppFolder,
    iconSize: Float,
    fontSize: Float,
    accentColor: Color,
    micaParams: MicaParams,
    isTransparencyEnabled: Boolean,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onAppLaunch: (AppModel) -> Unit,
    onRemoveApp: (AppModel) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var folderName by remember { mutableStateOf(folder.name) }
    var isEditingName by remember { mutableStateOf(false) }

    val overlayBg = if (isDark) Color.Black.copy(0.6f) else Color.Black.copy(0.3f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayBg)
            .zIndex(200f)
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            },
        contentAlignment = Alignment.Center
    ) {
        // ── Contenu du dossier ───────────────────────────────────────────────
        val folderShape = RoundedCornerShape(20.dp)

        Box(
            modifier = Modifier
                .width(300.dp)
                .wrapContentHeight()
                .pointerInput(Unit) { detectTapGestures { /* consomme le tap */ } }
                .then(
                    if (isTransparencyEnabled)
                        Modifier.fluentMicaComplex(
                            micaParams = micaParams,
                            enabled = true,
                            elevation = 0.dp,
                            shape = folderShape
                        )
                    else
                        Modifier.background(
                            if (isDark) Color(0xFF1E1E1E) else Color(0xFFF3F3F3),
                            folderShape
                        )
                )
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(0.12f) else Color.White.copy(0.6f),
                    folderShape
                )
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Nom du dossier éditable ──────────────────────────────────
                if (isEditingName) {
                    BasicTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        singleLine = true,
                        cursorBrush = SolidColor(accentColor),
                        textStyle = TextStyle(
                            color = micaParams.primaryColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                accentColor.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    // Valider avec un tap dehors — on sauvegarde le nom
                    LaunchedEffect(isEditingName) {
                        if (!isEditingName) onRename(folderName)
                    }
                } else {
                    Text(
                        text = folderName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = micaParams.primaryColor,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { isEditingName = true }
                                )
                            }
                    )
                }

                // ── Grille des apps dans le dossier ──────────────────────────
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled     = true
                ) {
                    items(folder.apps) { app ->
                        FolderAppItem(
                            app = app,
                            iconSize = iconSize * 0.75f,
                            fontSize = fontSize * 0.85f,
                            primaryColor = micaParams.primaryColor,
                            onTap = {
                                onDismiss()
                                onAppLaunch(app)
                            },
                            onLongPress = { onRemoveApp(app) }
                        )
                    }
                }

                // ── Hint ─────────────────────────────────────────────────────
                Text(
                    text = "Appui long sur une app pour la retirer",
                    fontSize = 11.sp,
                    color = micaParams.primaryColor.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── App item dans le dossier ouvert ──────────────────────────────────────────

@Composable
private fun FolderAppItem(
    app: AppModel,
    iconSize: Float,
    fontSize: Float,
    primaryColor: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (isPressed) 0.9f else 1f,
        spring(stiffness = Spring.StiffnessHigh),
        label = "item_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try { tryAwaitRelease() } finally { isPressed = false }
                    },
                    onTap = { onTap() },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
            }
    ) {
        InternalAppIcon(
            packageName = app.packageName,
            modifier = Modifier
                .size(iconSize.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Text(
            text = app.label,
            fontSize = fontSize.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = primaryColor,
            modifier = Modifier
                .padding(top = 3.dp)
                .width((iconSize + 8).dp)
        )
    }
}