package com.example.fluentui.ui.homescreen.widgets

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fluentui.ui.system.TileConfig
import com.example.fluentui.ui.system.TileSize
import com.example.fluentui.ui.theme.FluentFontFamily
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.filled.Airplane
import fluent.ui.system.icons.filled.Bluetooth
import fluent.ui.system.icons.filled.Flashlight
import fluent.ui.system.icons.filled.WiFi1
import fluent.ui.system.icons.regular.Airplane
import fluent.ui.system.icons.regular.BluetoothDisabled
import fluent.ui.system.icons.regular.FlashlightOff
import fluent.ui.system.icons.regular.WiFiOff
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "SystemWidget"

// ============================================================================
// DATA MODELS
// ============================================================================

private data class SystemState(
    val wifiEnabled:  Boolean = false,
    val wifiSsid:     String  = "",
    val btEnabled:    Boolean = false,
    val btDeviceName: String  = "",
    val airplaneMode: Boolean = false,
    val torchEnabled: Boolean = false
)

private data class ToggleAction(
    val activeIcon:   ImageVector,
    val inactiveIcon: ImageVector,
    val label:        String,
    val sublabel:     String,
    val isActive:     Boolean,
    val activeColor:  Color,
    val onClick:      () -> Unit
)

// ============================================================================
// WIDGET PRINCIPAL
// ============================================================================

@Composable
fun SystemWidget(
    config:       TileConfig,
    contentColor: Color,
    accentColor:  Color,
    modifier:     Modifier = Modifier
) {
    val context = LocalContext.current
    var state   by remember { mutableStateOf(readSystemState(context, false)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                state = readSystemState(context, state.torchEnabled)
            }
        }
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val torchCb = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                state = state.copy(torchEnabled = enabled)
            }
        }
        runCatching { cm.registerTorchCallback(torchCb, null) }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            runCatching { cm.unregisterTorchCallback(torchCb) }
        }
    }

    val actions = listOf(
        ToggleAction(
            activeIcon   = FluentIcons.Filled.WiFi1,
            inactiveIcon = FluentIcons.Regular.WiFiOff,
            label        = "Wi-Fi",
            sublabel     = when {
                state.wifiEnabled && state.wifiSsid.isNotBlank() -> state.wifiSsid
                state.wifiEnabled -> "Connecté"
                else -> "Désactivé"
            },
            isActive    = state.wifiEnabled,
            activeColor = accentColor,
            onClick     = { toggleWifi(context, state.wifiEnabled) }
        ),
        ToggleAction(
            activeIcon   = FluentIcons.Filled.Bluetooth,
            inactiveIcon = FluentIcons.Regular.BluetoothDisabled,
            label        = "Bluetooth",
            sublabel     = when {
                state.btEnabled && state.btDeviceName.isNotBlank() -> state.btDeviceName
                state.btEnabled -> "Activé"
                else -> "Désactivé"
            },
            isActive    = state.btEnabled,
            activeColor = accentColor,
            onClick     = { toggleBluetooth(context, state.btEnabled) }
        ),
        ToggleAction(
            activeIcon   = FluentIcons.Filled.Airplane,
            inactiveIcon = FluentIcons.Regular.Airplane,
            label        = "Mode Avion",
            sublabel     = if (state.airplaneMode) "Activé" else "Désactivé",
            isActive     = state.airplaneMode,
            activeColor  = Color(0xFFFF8C00),
            onClick      = { toggleAirplaneMode(context) }
        ),
        ToggleAction(
            activeIcon   = FluentIcons.Filled.Flashlight,
            inactiveIcon = FluentIcons.Regular.FlashlightOff,
            label        = "Torche",
            sublabel     = if (state.torchEnabled) "Allumée" else "Éteinte",
            isActive     = state.torchEnabled,
            activeColor  = Color(0xFFD13438),
            onClick      = { toggleTorch(context, state.torchEnabled) }
        )
    )

    Box(
        modifier         = modifier.fillMaxSize().padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (config.size) {
            TileSize.TINY -> TinyLayout(state, accentColor)
            TileSize.SMALL  -> ActionGrid(actions, columns = 2, contentColor = contentColor, size = TileSize.SMALL)
            TileSize.MEDIUM -> ActionGrid(actions, columns = 2, contentColor = contentColor, size = TileSize.MEDIUM)
            TileSize.LARGE  -> ActionGrid(actions, columns = 2, contentColor = contentColor, size = TileSize.LARGE)
        }
    }
}

// ============================================================================
// TINY
// ============================================================================

@Composable
private fun TinyLayout(state: SystemState, accentColor: Color) {
    val icon  = if (state.airplaneMode) FluentIcons.Filled.Airplane else FluentIcons.Filled.WiFi1
    val color = if (state.airplaneMode) Color(0xFFFF8C00) else accentColor
    Icon(icon, null, Modifier.size(24.dp), tint = color)
}

// ============================================================================
// GRILLE UNIFORME — une seule source de vérité pour toutes les tailles
// ============================================================================

@Composable
private fun ActionGrid(
    actions:      List<ToggleAction>,
    columns:      Int,
    contentColor: Color,
    size:         TileSize
) {
    // Dimensions adaptées par taille — valeurs mesurées pour que ça respire bien
    val iconBoxSize: Dp
    val iconSize:    Dp
    val iconRadius:  Dp
    val labelSp:     TextUnit
    val sublabelSp:  TextUnit
    val isLarge:     Boolean

    when (size) {
        TileSize.SMALL -> {
            iconBoxSize = 26.dp; iconSize = 13.dp; iconRadius = 6.dp
            labelSp = 10.sp; sublabelSp = 9.sp; isLarge = false
        }
        TileSize.MEDIUM -> {
            iconBoxSize = 32.dp; iconSize = 16.dp; iconRadius = 8.dp
            labelSp = 12.sp; sublabelSp = 10.sp; isLarge = false
        }
        TileSize.LARGE -> {
            iconBoxSize = 44.dp; iconSize = 22.dp; iconRadius = 22.dp
            labelSp = 13.sp; sublabelSp = 11.sp; isLarge = true
        }
        else -> {
            iconBoxSize = 26.dp; iconSize = 13.dp; iconRadius = 6.dp
            labelSp = 10.sp; sublabelSp = 9.sp; isLarge = false
        }
    }

    Column(
        modifier            = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        actions.chunked(columns).forEach { rowActions ->
            Row(
                modifier              = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowActions.forEach { action ->
                    ToggleTile(
                        action       = action,
                        contentColor = contentColor,
                        isLarge      = isLarge,
                        iconBoxSize  = iconBoxSize,
                        iconSize     = iconSize,
                        iconRadius   = iconRadius,
                        labelSp      = labelSp,
                        sublabelSp   = sublabelSp,
                        modifier     = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                repeat(columns - rowActions.size) {
                    Spacer(Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

// ============================================================================
// TUILE TOGGLE
// ============================================================================

@Composable
private fun ToggleTile(
    action:       ToggleAction,
    contentColor: Color,
    isLarge:      Boolean,
    iconBoxSize:  Dp,
    iconSize:     Dp,
    iconRadius:   Dp,
    labelSp:      TextUnit,
    sublabelSp:   TextUnit,
    modifier:     Modifier = Modifier
) {
    val scope     = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    val currentIcon = if (action.isActive || isPressed) action.activeIcon else action.inactiveIcon

    val bgColor by animateColorAsState(
        if (action.isActive) action.activeColor.copy(0.18f) else Color.White.copy(0.05f),
        tween(150), label = "bg"
    )
    val iconBgColor by animateColorAsState(
        if (action.isActive) action.activeColor else Color.White.copy(0.10f),
        tween(150), label = "iconBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (action.isActive) {
            if (action.activeColor.luminance() > 0.5f) Color.Black else Color.White
        } else Color.White,
        animationSpec = tween(150), label = "tint"
    )
    val iconScale by animateFloatAsState(
        if (isPressed) 0.80f else 1f, tween(100), label = "scale"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable {
                scope.launch { isPressed = true; delay(150); isPressed = false }
                action.onClick()
            }
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        if (isLarge) {
            // LARGE — icône centrée en haut, textes en bas à gauche
            Column(
                modifier            = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier         = Modifier
                        .size(iconBoxSize)
                        .clip(RoundedCornerShape(iconRadius))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        currentIcon, null,
                        Modifier.size(iconSize).scale(iconScale),
                        tint = iconTint
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text       = action.label,
                        fontFamily = FluentFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = labelSp,
                        color      = Color.White,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text       = action.sublabel,
                        fontFamily = FluentFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize   = sublabelSp,
                        color      = Color.White.copy(0.55f),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // SMALL / MEDIUM — icône en haut à gauche, textes en bas
            Column(
                modifier            = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier         = Modifier
                        .size(iconBoxSize)
                        .clip(RoundedCornerShape(iconRadius))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        currentIcon, null,
                        Modifier.size(iconSize).scale(iconScale),
                        tint = iconTint
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text       = action.label,
                        fontFamily = FluentFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = labelSp,
                        color      = Color.White,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text       = action.sublabel,
                        fontFamily = FluentFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize   = sublabelSp,
                        color      = Color.White.copy(0.55f),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ============================================================================
// TOGGLES
// ============================================================================

private fun toggleTorch(context: Context, currentlyEnabled: Boolean) {
    runCatching {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cm.setTorchMode(cm.cameraIdList.firstOrNull() ?: return, !currentlyEnabled)
    }.onFailure { Log.e(TAG, "toggleTorch", it) }
}

private fun toggleAirplaneMode(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Suppress("DEPRECATION")
private fun toggleWifi(context: Context, currentlyEnabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            context.startActivity(Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    } else {
        runCatching {
            (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                .isWifiEnabled = !currentlyEnabled
        }.onFailure {
            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private fun toggleBluetooth(context: Context, currentlyEnabled: Boolean) {
    val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        context.checkSelfPermission("android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_GRANTED
    else true

    if (!hasPerm) {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.startActivity(
                Intent(if (currentlyEnabled) "android.settings.BLUETOOTH_SETTINGS" else Settings.ACTION_BLUETOOTH_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            @Suppress("DEPRECATION")
            val bt = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            if (currentlyEnabled) bt?.disable() else bt?.enable()
        }
    }.onFailure {
        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

// ============================================================================
// STATE READER
// ============================================================================

@Suppress("DEPRECATION")
private fun readWifiSsid(context: Context, wifiMgr: WifiManager): String {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return ""
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return ""
        }
        wifiMgr.connectionInfo?.ssid
            ?.removePrefix("\"")?.removeSuffix("\"")
            ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
            .orEmpty()
    }.getOrDefault("")
}

private fun readSystemState(context: Context, currentTorchState: Boolean): SystemState {
    return runCatching {
        val wifiMgr     = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiEnabled = wifiMgr.isWifiEnabled
        val wifiSsid    = if (wifiEnabled) readWifiSsid(context, wifiMgr) else ""

        val btMgr     = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val hasBtPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.checkSelfPermission("android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_GRANTED
        else true
        val btEnabled = hasBtPerm && try { btMgr.adapter?.isEnabled == true } catch (e: SecurityException) { false }
        val btDevice  = if (btEnabled && hasBtPerm) {
            try { btMgr.adapter?.bondedDevices?.firstOrNull()?.name.orEmpty() } catch (e: SecurityException) { "" }
        } else ""

        val airplaneMode = Settings.Global.getInt(
            context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0

        SystemState(wifiEnabled, wifiSsid, btEnabled, btDevice, airplaneMode, currentTorchState)
    }.getOrElse {
        Log.e(TAG, "Error reading system state", it)
        SystemState(torchEnabled = currentTorchState)
    }
}