package com.example.fluentui.ui.components

import android.annotation.SuppressLint
import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.fluentui.R
import com.example.fluentui.wallpaper.MicaParams
import com.example.fluentui.wallpaper.fluentMicaComplex
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.Delete
import fluent.ui.system.icons.regular.Info
import fluent.ui.system.icons.regular.Pin

// ==============================================================================
// CUSTOM FLUENT SHADOW (V2 - Ombre directionnelle propre, sans "halo sale")
// ==============================================================================
fun Modifier.fluentDropShadow(
    color: Color,
    borderRadius: Dp,
    blurRadius: Dp,
    offsetY: Dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.dp
) = this.drawBehind {
    this.drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = color.toArgb()

        if (blurRadius != 0.dp) {
            frameworkPaint.maskFilter = BlurMaskFilter(blurRadius.toPx(), BlurMaskFilter.Blur.NORMAL)
        }

        val spreadPx = spread.toPx()

        canvas.drawRoundRect(
            left = offsetX.toPx() - spreadPx,
            top = offsetY.toPx() - spreadPx,
            right = size.width + offsetX.toPx() + spreadPx,
            bottom = size.height + offsetY.toPx() + spreadPx,
            radiusX = borderRadius.toPx(),
            radiusY = borderRadius.toPx(),
            paint = paint
        )
    }
}

// ==============================================================================
// MAIN CONTEXT MENU
// ==============================================================================

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
    val iconColor = textColor.copy(alpha = 0.8f)

    // CALCUL POSITION
    val screenHeight = context.resources.displayMetrics.heightPixels
    val showBelow = (screenHeight - iconPosition.y - iconHeight) > with(density) { 200.dp.toPx() }

    val vOffset = if (showBelow) {
        with(density) { (iconSize + 4).dp.roundToPx() }
    } else {
        with(density) { -170.dp.roundToPx() }
    }

    // DESIGN SYSTEM WINDOWS 11
    val cornerRadius = 8.dp
    val menuShape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isDark) Color.White.copy(0.12f) else Color.Black.copy(0.08f)

    // Matériau de base un peu plus opaque pour le mode solide
    val solidWindowsColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)

    // ANIMATION FLUIDE
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(150),
        label = "alpha"
    )

    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, vOffset),
        onDismissRequest = {
            isVisible = false
            onDismiss()
        },
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                }
                .width(260.dp)
                .fluentDropShadow(
                    color = Color.Black.copy(alpha = if (isDark) 0.35f else 0.15f),
                    borderRadius = cornerRadius,
                    blurRadius = 16.dp,
                    offsetY = 8.dp,
                    spread = (-4).dp
                )
                .then(
                    if (isTransparencyEnabled) {
                        Modifier.fluentMicaComplex(
                            micaParams = micaParams,
                            enabled = true,
                            elevation = 0.dp,
                            shape = menuShape
                        )
                    } else {
                        Modifier.background(solidWindowsColor, menuShape)
                    }
                )
                .border(1.dp, borderColor, menuShape)
                .clip(menuShape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                ContextMenuItem(
                    icon = FluentIcons.Regular.Pin,
                    text = stringResource(R.string.menu_pin_start),
                    textColor = textColor,
                    iconColor = iconColor,
                    onClick = { onPin(); onDismiss() }
                )

                ContextMenuItem(
                    icon = FluentIcons.Regular.Info,
                    text = stringResource(R.string.menu_app_info),
                    textColor = textColor,
                    iconColor = iconColor,
                    onClick = {
                        AppActions.openAppInfo(context, app.packageName)
                        onDismiss()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
                    thickness = 1.dp,
                    color = textColor.copy(alpha = 0.1f)
                )

                ContextMenuItem(
                    icon = FluentIcons.Regular.Delete,
                    text = stringResource(R.string.menu_uninstall_app),
                    textColor = Color(0xFFE53935),
                    iconColor = Color(0xFFE53935),
                    onClick = {
                        AppActions.uninstallApp(context, app.packageName)
                        onDismiss()
                    }
                )
            }
        }
    }
}

// ==============================================================================
// MENU ITEM
// ==============================================================================

@Composable
private fun ContextMenuItem(
    icon: ImageVector, text: String, textColor: Color, iconColor: Color, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = text,
            color = textColor,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}