package com.example.fluentui.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.fluentui.R
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.AlertBadge
import fluent.ui.system.icons.regular.CheckmarkCircle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OobeStep { GREETING_ANIM, PERMISSIONS, DEFAULT_LAUNCHER, FINALIZING }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupWizardScreen(
    viewModel: AppViewModel,
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    val isCompactHeight = configuration.screenHeightDp < 640
    val isCompactWidth = configuration.screenWidthDp < 360

    var currentStep by remember { mutableStateOf(OobeStep.GREETING_ANIM) }
    val isDark = isSystemInDarkTheme()

    BackHandler(enabled = currentStep != OobeStep.GREETING_ANIM && currentStep != OobeStep.FINALIZING) {
        if (currentStep.ordinal > 0) {
            currentStep = OobeStep.values()[currentStep.ordinal - 1]
        }
    }

    var hasStoragePermission by remember { mutableStateOf(checkStoragePermission(context)) }
    var hasContactsPermission by remember { mutableStateOf(checkContactsPermission(context)) }
    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasContactsPermission = isGranted }

    val storagePermissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasStoragePermission = isGranted }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStoragePermission = checkStoragePermission(context)
                hasContactsPermission = checkContactsPermission(context)
                hasNotificationPermission = checkNotificationPermission(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val backgroundWash = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.30f)
    val cardBackground = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.70f) else Color.White.copy(alpha = 0.75f)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f)
    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val accentColor = Color(0xFF0067C0)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Image(
            painter = painterResource(id = R.drawable.windows_11_1_mattbirchler),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(radius = 80.dp)
        )
        Box(modifier = Modifier.fillMaxSize().background(backgroundWash))

        AnimatedVisibility(
            visible = currentStep != OobeStep.GREETING_ANIM,
            enter = fadeIn(tween(1000, delayMillis = 400)) + scaleIn(tween(1000, delayMillis = 400), initialScale = 0.95f),
            exit = fadeOut(tween(400)) + scaleOut(tween(400), targetScale = 0.95f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            // 🔑 CARTE ADAPTATIVE : prend tout l'écran sur petit device
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isCompactWidth) 1f else 0.90f)
                    .fillMaxHeight(if (isCompactHeight) 1f else 0.85f)
                    .then(
                        if (isCompactHeight || isCompactWidth) Modifier
                        else Modifier.shadow(24.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                    )
                    .clip(RoundedCornerShape(if (isCompactHeight) 0.dp else 12.dp))
                    .background(cardBackground)
                    .border(
                        1.dp,
                        cardBorder,
                        RoundedCornerShape(if (isCompactHeight) 0.dp else 12.dp)
                    )
                    .windowInsetsPadding(WindowInsets.systemBars) // 🔑 évite collage aux barres système
            ) {
                if (currentStep != OobeStep.FINALIZING && currentStep != OobeStep.GREETING_ANIM) {
                    IconButton(
                        onClick = {
                            if (currentStep.ordinal > 1) {
                                currentStep = OobeStep.values()[currentStep.ordinal - 1]
                            }
                        },
                        modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                }

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            (fadeIn(tween(300)) + slideInHorizontally(tween(300), initialOffsetX = { 50 }))
                                .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(300), targetOffsetX = { -50 }))
                        } else {
                            (fadeIn(tween(300)) + slideInHorizontally(tween(300), initialOffsetX = { -50 }))
                                .togetherWith(fadeOut(tween(150)) + slideOutHorizontally(tween(300), targetOffsetX = { 50 }))
                        }
                    },
                    label = "CardNavigation",
                    modifier = Modifier.fillMaxSize().padding(
                        top = if (isCompactHeight) 56.dp else 72.dp,
                        bottom = 0.dp,
                        start = if (isCompactWidth) 16.dp else 32.dp,
                        end = if (isCompactWidth) 16.dp else 32.dp
                    )
                ) { step ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (step) {
                            OobeStep.PERMISSIONS -> PermissionsPage(
                                context = context,
                                textColor = textColor,
                                accentColor = accentColor,
                                hasStorage = hasStoragePermission,
                                hasContacts = hasContactsPermission,
                                hasNotifications = hasNotificationPermission,
                                hasOverlay = hasOverlayPermission,
                                isCompact = isCompactHeight,
                                onRequestStorage = { storagePermissionLauncher.launch(storagePermissionToRequest) },
                                onRequestContacts = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                onNext = { currentStep = OobeStep.DEFAULT_LAUNCHER }
                            )
                            OobeStep.DEFAULT_LAUNCHER -> DefaultLauncherPage(
                                context = context,
                                textColor = textColor,
                                accentColor = accentColor,
                                isCompact = isCompactHeight,
                                onNext = { currentStep = OobeStep.FINALIZING }
                            )
                            OobeStep.FINALIZING -> FinalizingPage(viewModel, textColor, accentColor, onSetupComplete)
                            else -> Unit
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = currentStep == OobeStep.GREETING_ANIM,
            exit = fadeOut(animationSpec = tween(1200))
        ) {
            SequentialGreetingOverlay(
                isCompact = isCompactHeight,
                onGreetingFinished = { currentStep = OobeStep.PERMISSIONS }
            )
        }
    }
}

@Composable
private fun SequentialGreetingOverlay(
    isCompact: Boolean,
    onGreetingFinished: () -> Unit
) {
    var phase by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(400)
        phase = 1
        delay(1200)
        phase = 2
    }

    val bonjourAlpha by animateFloatAsState(
        targetValue = if (phase in 1..2) 1f else 0f,
        animationSpec = tween(1000, easing = LinearEasing), label = "bAlpha"
    )
    val bonjourOffset by animateDpAsState(
        targetValue = if (phase >= 2) (-40).dp else 0.dp,
        animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "bOffset"
    )
    val subtextAlpha by animateFloatAsState(
        targetValue = if (phase == 2) 1f else 0f,
        animationSpec = tween(800, delayMillis = 300, easing = LinearEasing), label = "sAlpha"
    )
    val subtextOffset by animateDpAsState(
        targetValue = if (phase >= 2) 20.dp else 40.dp,
        animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "sOffset"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (phase == 2) 1f else 0f,
        animationSpec = tween(800, delayMillis = 600, easing = LinearEasing), label = "btnAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.oobe_greeting_title),
            color = Color.White.copy(alpha = bonjourAlpha),
            fontSize = if (isCompact) 38.sp else 52.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = 1.sp,
            modifier = Modifier.offset(y = bonjourOffset)
        )
        Text(
            text = stringResource(R.string.oobe_greeting_subtitle),
            color = Color.White.copy(alpha = subtextAlpha * 0.7f),
            fontSize = if (isCompact) 16.sp else 20.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp,
            modifier = Modifier.offset(y = subtextOffset)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = if (isCompact) 32.dp else 64.dp)
                .graphicsLayer { alpha = buttonAlpha }
        ) {
            Button(
                onClick = {
                    if (phase == 2) {
                        coroutineScope.launch {
                            phase = 3
                            delay(800)
                            onGreetingFinished()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.oobe_btn_continue),
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * PermissionsPage REFACTORÉE :
 * - Contenu scrollable
 * - Bouton "Suivant" STICKY en bas (jamais hors écran)
 */
@Composable
private fun PermissionsPage(
    context: Context,
    textColor: Color,
    accentColor: Color,
    hasStorage: Boolean,
    hasContacts: Boolean,
    hasNotifications: Boolean,
    hasOverlay: Boolean,
    isCompact: Boolean,
    onRequestStorage: () -> Unit,
    onRequestContacts: () -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.oobe_permissions_title),
                fontSize = if (isCompact) 22.sp else 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Optionnel - Vous pouvez continuer sans autoriser certaines permissions",
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 32.dp))

            FluentPermissionRow(
                title = stringResource(R.string.oobe_perm_storage_title),
                subtitle = if (hasStorage) stringResource(R.string.oobe_perm_storage_granted)
                else stringResource(R.string.oobe_perm_storage_req),
                textColor = textColor, accentColor = accentColor, isGranted = hasStorage,
                onClick = { if (!hasStorage) onRequestStorage() }
            )
            Spacer(modifier = Modifier.height(10.dp))

            FluentPermissionRow(
                title = stringResource(R.string.oobe_perm_contacts_title),
                subtitle = if (hasContacts) stringResource(R.string.oobe_perm_contacts_granted)
                else stringResource(R.string.oobe_perm_contacts_req),
                textColor = textColor, accentColor = accentColor, isGranted = hasContacts,
                onClick = { if (!hasContacts) onRequestContacts() }
            )
            Spacer(modifier = Modifier.height(10.dp))

            FluentPermissionRow(
                title = stringResource(R.string.oobe_perm_notif_title),
                subtitle = if (hasNotifications) stringResource(R.string.oobe_perm_notif_granted)
                else stringResource(R.string.oobe_perm_notif_req),
                textColor = textColor, accentColor = accentColor, isGranted = hasNotifications,
                onClick = {
                    if (!hasNotifications) {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            FluentPermissionRow(
                title = "Action Center Overlay",
                subtitle = if (hasOverlay) "Granted" else "Required to swipe down from any app",
                textColor = textColor, accentColor = accentColor, isGranted = hasOverlay,
                onClick = {
                    if (!hasOverlay) {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FluentPrimaryButton(
                text = stringResource(R.string.oobe_btn_next),
                accentColor = accentColor,
                enabled = true,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun DefaultLauncherPage(
    context: Context,
    textColor: Color,
    accentColor: Color,
    isCompact: Boolean,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.oobe_default_title),
                fontSize = if (isCompact) 22.sp else 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.oobe_default_desc),
                fontSize = 15.sp,
                color = textColor.copy(alpha = 0.7f),
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(if (isCompact) 24.dp else 48.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                    .clickable { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.oobe_btn_open_settings),
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FluentPrimaryButton(
                text = stringResource(R.string.oobe_btn_finish),
                accentColor = accentColor,
                enabled = true,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun FinalizingPage(
    viewModel: AppViewModel,
    textColor: Color,
    accentColor: Color,
    onComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.refreshWallpaperVisuals()
        delay(2000)
        onComplete()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = accentColor, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.oobe_finalizing),
            color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FluentPrimaryButton(
    text: String,
    accentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (enabled) accentColor else Color.Gray.copy(alpha = 0.2f)
    val contentColor = if (enabled) Color.White else Color.Black.copy(alpha = 0.4f)

    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 36.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Text(text, color = contentColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun FluentPermissionRow(
    title: String,
    subtitle: String,
    textColor: Color,
    accentColor: Color,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isGranted) accentColor.copy(alpha = 0.1f) else textColor.copy(alpha = 0.04f))
            .clickable(enabled = !isGranted) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) FluentIcons.Regular.CheckmarkCircle else FluentIcons.Regular.AlertBadge,
            contentDescription = null,
            tint = if (isGranted) accentColor else textColor.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = textColor.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}

private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun checkContactsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
}

private fun checkNotificationPermission(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
