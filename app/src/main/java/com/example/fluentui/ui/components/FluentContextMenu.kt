package com.example.fluentui.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.example.fluentui.ui.components.AppActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.fluentMicaComplex
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Info
import fluent.ui.system.icons.regular.Pin

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun FluentContextMenu(
    app: AppModel,
    iconPosition: Offset,
    iconHeight: Float,
    iconSize: Float,
    enabled: Boolean = true,
    onDismiss: () -> Unit,
    isDark: Boolean,
    onPin: () -> Unit,
    micaParams: MicaParams?,
    isTransparencyEnabled: Boolean
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // COULEURS
    val textColor = micaParams?.contentColor ?: (if(isDark) Color.White else Color.Black)
    // Icônes un peu plus discrètes que le texte pour la hiérarchie visuelle
    val iconColor = textColor.copy(alpha = 0.8f)

    // CALCUL POSITION
    val screenHeight = context.resources.displayMetrics.heightPixels
    val showBelow = (screenHeight - iconPosition.y - iconHeight) > with(density) { 200.dp.toPx() }

    val vOffset = if (showBelow) {
        with(density) { (iconSize + 4).dp.roundToPx() }
    } else {
        with(density) { -170.dp.roundToPx() } // Ajusté pour la nouvelle taille
    }

    // DESIGN SYSTEM WINDOWS 11
    val cornerRadius = 10.dp // Arrondi standard Windows 11
    val menuShape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f)
    val solidWindowsColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)

    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, vOffset),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Column(
            modifier = Modifier
                .width(260.dp) // <-- ELARGISSEMENT (Évite le texte coupé "...")
                .then(
                    if (isTransparencyEnabled) {
                        Modifier.fluentMicaComplex(
                            micaParams = micaParams,
                            enabled = true,
                            elevation = 24.dp,
                            shape = menuShape
                        )
                    } else {
                        Modifier
                            .shadow(24.dp, menuShape)
                            .background(solidWindowsColor, menuShape)
                            .border(1.dp, borderColor, menuShape)
                    }
                )
                .padding(5.dp) // Petit padding externe (contour du menu)
        ) {
            // ITEM 1 : Épingler
            ContextMenuItem(
                icon = FluentIcons.Regular.Pin,
                text = "Épingler au menu Démarrer",
                textColor = textColor,
                iconColor = iconColor,
                onClick = { onPin(); onDismiss() }
            )

            // Espacement très fin entre les items
            Spacer(Modifier.height(2.dp))

            // ITEM 2 : À PROPOS
            ContextMenuItem(
                icon = FluentIcons.Regular.Info,
                text = "À propos de l'appli",
                textColor = textColor,
                iconColor = iconColor,
                onClick = { AppActions.openAppInfo(context, app.packageName); onDismiss() }
            )

            // SÉPARATEUR (Style Windows 11 : fin et subtil)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 5.dp, horizontal = 10.dp),
                thickness = 1.dp,
                color = textColor.copy(alpha = 0.08f)
            )

            // ITEM 3 : Désinstaller
            ContextMenuItem(
                icon = FluentIcons.Regular.Delete,
                text = "Désinstaller",
                textColor = textColor,
                iconColor = iconColor,
                // On garde la couleur normale ou on met du rouge si tu veux un style "Danger"
                onClick = {
                    AppActions.uninstallApp(context, app.packageName) // <-- Appelle le fichier qu'on vient de créer
                    onDismiss() // Ferme le menu
                }            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector, text: String, textColor: Color, iconColor: Color, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp) // Hauteur compacte mais tactile
            .clip(RoundedCornerShape(4.dp)) // Arrondi au survol (Hover effect style)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp), // Marge interne gauche/droite
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. ICÔNE À GAUCHE (Standard Windows)
        // Box fixe pour aligner parfaitement le texte même si l'icône change
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp) // Icône fine et élégante
            )
        }

        Spacer(Modifier.width(12.dp)) // Espace entre icône et texte

        // 2. TEXTE
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp, // Taille standard lisible
            fontWeight = FontWeight.Normal, // Pas de gras inutile, c'est plus propre
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f) // Prend toute la place restante
        )
    }
}