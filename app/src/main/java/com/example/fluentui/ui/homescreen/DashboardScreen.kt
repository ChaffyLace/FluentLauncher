package com.example.fluentui.ui.homescreen

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.fluentui.ui.components.AppViewModel
import com.example.fluentui.ui.motion.FluentDashboardMotion
import com.example.fluentui.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardPage(
    viewModel: AppViewModel,
    theme: AppTheme = AppTheme.WINDOWS_DARK
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // État pour gérer l'animation globale de "Recul" lors du lancement d'une app
    var isAppLaunching by remember { mutableStateOf(false) }

    // Récupération réactive des données
    // On observe directement le Flow pour être toujours à jour
    val micaParams by viewModel.micaParams.collectAsState()

    // On utilise la liste observable du ViewModel pour que les changements (move/resize) soient instantanés
    val tiles = viewModel.dashboardTiles

    // Wrapper d'animation globale : Tout le dashboard réagit au lancement d'une app
    FluentDashboardMotion(isAppLaunching = isAppLaunching) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Important : On laisse de la place pour la barre de statut transparente
                .statusBarsPadding()
        ) {
            DashboardGrid(
                tiles = tiles,
                micaParams = micaParams,
                isDark = isSystemInDarkTheme(),
                viewModel = viewModel,
                // Callback enrichi : On gère l'animation ET le lancement
                onTileClick = { tile ->
                    if (!isAppLaunching && tile.packageName != null) {
                        // 1. Déclencher l'animation visuelle
                        isAppLaunching = true

                        // 2. Lancer l'application
                        val intent = context.packageManager.getLaunchIntentForPackage(tile.packageName)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            // Petite pause pour laisser l'utilisateur voir l'animation "Press" de la tuile
                            scope.launch {
                                delay(150) // Délai perceptuel pour le feedback tactile
                                context.startActivity(intent)

                                // 3. Reset de l'état après un délai de sécurité
                                // (Pour que l'anim inverse se joue quand on revient)
                                delay(800)
                                isAppLaunching = false
                            }
                        } else {
                            // Si l'app est introuvable (désinstallée ?), on annule l'anim
                            isAppLaunching = false
                        }
                    }
                },
                // On passe l'état de lancement à la grille pour qu'elle puisse
                // animer spécifiquement la tuile cliquée (Zoom In)
                isLaunchingState = isAppLaunching
            )
        }
    }
}