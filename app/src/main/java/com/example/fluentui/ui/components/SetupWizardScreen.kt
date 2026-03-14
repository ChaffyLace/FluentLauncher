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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.fluentui.R
import fluent.ui.system.icons.FluentIcons
import fluent.ui.system.icons.regular.AlertBadge
import fluent.ui.system.icons.regular.CheckmarkCircle
import fluent.ui.system.icons.regular.ChevronRight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PRODUCTION-READY OOBE
 * Implements a sequential spatial animation for the greeting, perfectly matching
 * flagship OS boot experiences (Bonjour -> shifts up -> Let's set up -> User Action -> dissolves).
 */
enum class OobeStep { GREETING_ANIM, PERMISSIONS, DEFAULT_LAUNCHER, FINALIZING }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupWizardScreen(
    viewModel: AppViewModel,
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // START WITH THE CINE GREETING
    var currentStep by remember { mutableStateOf(OobeStep.GREETING_ANIM) }
    val isDark = isSystemInDarkTheme()

    // --- SYSTEM BACK HANDLER ---
    BackHandler(enabled = currentStep != OobeStep.GREETING_ANIM && currentStep != OobeStep.FINALIZING) {
        currentStep = OobeStep.values()[currentStep.ordinal - 1]
    }

    // --- PERMISSION STATES ---
    var hasStoragePermission by remember { mutableStateOf(checkStoragePermission(context)) }
    var hasContactsPermission by remember { mutableStateOf(checkContactsPermission(context)) }

    // Launcher for standard runtime permissions (Contacts)
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
    }

    // Auto-refresh permissions when returning from Android Settings (for Storage)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStoragePermission = checkStoragePermission(context)
                hasContactsPermission = checkContactsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // --- WINDOWS 11 MICA & ACRYLIC TUNING ---
    val backgroundWash = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.30f)
    val cardBackground = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.70f) else Color.White.copy(alpha = 0.75f)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f)
    val textColor = if (isDark) Color.White else Color(0xFF1A1A1A)
    val accentColor = Color(0xFF0067C0)

    // Root Container (Pure Black for cinematic effect)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ====================================================================
        // Z-LAYER 0: THE MICA DESKTOP (Always present, blurred in background)
        // ====================================================================
        Image(
            painter = painterResource(id = R.drawable.windows_11_1_mattbirchler),
            contentDescription = "OOBE Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(radius = 80.dp)
        )
        Box(modifier = Modifier.fillMaxSize().background(backgroundWash))

        // ====================================================================
        // Z-LAYER 1: THE ACRYLIC SETUP CARD
        // ====================================================================
        AnimatedVisibility(
            visible = currentStep != OobeStep.GREETING_ANIM,
            enter = fadeIn(tween(1000, delayMillis = 400)) + scaleIn(tween(1000, delayMillis = 400), initialScale = 0.95f),
            exit = fadeOut(tween(400)) + scaleOut(tween(400), targetScale = 0.95f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.85f)
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(cardBackground)
                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            ) {
                if (currentStep != OobeStep.FINALIZING) {
                    IconButton(
                        onClick = { currentStep = OobeStep.values()[currentStep.ordinal - 1] },
                        modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
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
                    modifier = Modifier.fillMaxSize().padding(top = 72.dp, bottom = 32.dp, start = 32.dp, end = 32.dp)
                ) { step ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (step) {
                            OobeStep.PERMISSIONS -> PermissionsPage(
                                context = context,
                                textColor = textColor,
                                accentColor = accentColor,
                                hasStorage = hasStoragePermission,
                                hasContacts = hasContactsPermission,
                                onRequestContacts = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                onNext = { currentStep = OobeStep.DEFAULT_LAUNCHER }
                            )
                            OobeStep.DEFAULT_LAUNCHER -> DefaultLauncherPage(context, textColor, accentColor) { currentStep = OobeStep.FINALIZING }
                            OobeStep.FINALIZING -> FinalizingPage(viewModel, textColor, accentColor, onSetupComplete)
                            else -> Unit
                        }
                    }
                }
            }
        }

        // ====================================================================
        // Z-LAYER 2: THE BLACK "HELLO" CURTAIN WITH BUTTON
        // ====================================================================
        AnimatedVisibility(
            visible = currentStep == OobeStep.GREETING_ANIM,
            exit = fadeOut(animationSpec = tween(1200)) // Drops the curtain slowly
        ) {
            SequentialGreetingOverlay(
                onGreetingFinished = { currentStep = OobeStep.PERMISSIONS }
            )
        }
    }
}

// --- SEQUENTIAL INTERACTIVE GREETING ---
@Composable
private fun SequentialGreetingOverlay(onGreetingFinished: () -> Unit) {
    // 0 = Black, 1 = Bonjour, 2 = Bonjour moves + Subtext + Button, 3 = Fade Out
    var phase by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(400)
        phase = 1 // Show "Bonjour."
        delay(1200)
        phase = 2 // Move up, show "Let's set up your device" and the button
    }

    // Mathematical choregraphy of components
    val bonjourAlpha by animateFloatAsState(
        targetValue = if (phase in 1..2) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing), label = "bAlpha"
    )

    val bonjourOffset by animateDpAsState(
        targetValue = if (phase >= 2) (-40).dp else 0.dp,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing), label = "bOffset"
    )

    val subtextAlpha by animateFloatAsState(
        targetValue = if (phase == 2) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 300, easing = LinearEasing), label = "sAlpha"
    )

    val subtextOffset by animateDpAsState(
        targetValue = if (phase >= 2) 20.dp else 40.dp,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing), label = "sOffset"
    )

    val buttonAlpha by animateFloatAsState(
        targetValue = if (phase == 2) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 600, easing = LinearEasing), label = "btnAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // "Bonjour."
        Text(
            text = "Bonjour.",
            color = Color.White.copy(alpha = bonjourAlpha),
            fontSize = 52.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = 1.sp,
            modifier = Modifier.offset(y = bonjourOffset)
        )

        // Subtitle text
        Text(
            text = "Let's set up your device.",
            color = Color.White.copy(alpha = subtextAlpha * 0.7f), // 70% opacity for hierarchy
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp,
            modifier = Modifier.offset(y = subtextOffset)
        )

        // High-contrast Continue Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .graphicsLayer { alpha = buttonAlpha }
        ) {
            Button(
                onClick = {
                    if (phase == 2) {
                        coroutineScope.launch {
                            phase = 3 // Trigger fade out
                            delay(800) // Wait for text and button to vanish
                            onGreetingFinished() // Drop the black curtain
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 14.dp)
            ) {
                Text("Continue", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

// --- SETUP CARD PAGES ---

@Composable
private fun PermissionsPage(
    context: Context,
    textColor: Color,
    accentColor: Color,
    hasStorage: Boolean,
    hasContacts: Boolean,
    onRequestContacts: () -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("System Access", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text("To display your apps, wallpapers, and contacts, Fluent requires these specific permissions.", fontSize = 15.sp, color = textColor.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(48.dp))

        // Storage Permission Row
        FluentPermissionRow(
            title = "Storage & Files",
            subtitle = if (hasStorage) "Granted" else "Required to load wallpapers",
            textColor = textColor, accentColor = accentColor, isGranted = hasStorage,
            onClick = {
                if (!hasStorage) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply { data = Uri.parse("package:${context.packageName}") })
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Contacts Permission Row
        FluentPermissionRow(
            title = "Contacts & Search",
            subtitle = if (hasContacts) "Granted" else "Required for smart search & quick calls",
            textColor = textColor, accentColor = accentColor, isGranted = hasContacts,
            onClick = {
                if (!hasContacts) {
                    onRequestContacts() // Triggers the Android dialog
                }
            }
        )

        Spacer(modifier = Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            // Require BOTH permissions to proceed to maintain standard OOBE constraints
            FluentPrimaryButton("Next", accentColor, enabled = hasStorage && hasContacts, onClick = onNext)
        }
    }
}

@Composable
private fun DefaultLauncherPage(
    context: Context, textColor: Color, accentColor: Color,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Make it default", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Set Fluent as your default Home application to complete the setup process and unlock all features.", fontSize = 15.sp, color = textColor.copy(alpha = 0.7f), lineHeight = 22.sp)
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                .clickable { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Open App Settings", fontWeight = FontWeight.SemiBold, color = accentColor)
        }

        Spacer(modifier = Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FluentPrimaryButton("Finish", accentColor, enabled = true, onClick = onNext)
        }
    }
}

@Composable
private fun FinalizingPage(viewModel: AppViewModel, textColor: Color, accentColor: Color, onComplete: () -> Unit) {
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
        Text("Just a moment...", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

// --- FLUENT UI ATOMS ---

@Composable
private fun FluentPrimaryButton(text: String, accentColor: Color, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val containerColor = if (enabled) accentColor else Color.Gray.copy(alpha = 0.2f)
    val contentColor = if (enabled) Color.White else Color.Black.copy(alpha = 0.4f)
    Button(
        onClick = onClick, enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, disabledContainerColor = containerColor),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 36.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Text(text, color = contentColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun FluentPermissionRow(title: String, subtitle: String, textColor: Color, accentColor: Color, isGranted: Boolean, onClick: () -> Unit) {
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
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        android.os.Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

// Nouvelle fonction de vérification pour les contacts
private fun checkContactsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
}