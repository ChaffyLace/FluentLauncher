package com.example.fluentui.ui.settings

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fluentui.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val settingsManager = SettingsManager(context)

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsManager.columnCount,
                settingsManager.rowCount,
                settingsManager.iconSize,
                settingsManager.fontSize,
                settingsManager.verticalSpacing,
                settingsManager.showLabels,
                settingsManager.isVerticalScroll,
                settingsManager.accentColor,
                settingsManager.transparencyEnabled,
                settingsManager.wallpaperType,
                settingsManager.wallpaperValue,
                settingsManager.hiddenApps
            ) { params ->
                val cols = params[0] as Int
                val rows = params[1] as Int
                val iSize = params[2] as Float
                val fSize = params[3] as Float
                val vSpace = params[4] as Float
                val labels = params[5] as Boolean
                val vert = params[6] as Boolean
                val accent = params[7] as Int
                val transp = params[8] as Boolean
                val wpType = params[9] as String
                val wpVal = params[10] as String
                @Suppress("UNCHECKED_CAST")
                val hidden = params[11] as Set<String>

                SettingsState(
                    columnCount = cols,
                    rowCount = rows,
                    iconSize = iSize,
                    fontSize = fSize,
                    verticalSpacing = vSpace,
                    showLabels = labels,
                    isVertical = vert,
                    accentColor = Color(accent),
                    transparencyEnabled = transp,
                    wallpaperType = wpType,
                    wallpaperValue = wpVal,
                    hiddenApps = hidden,
                    // On garde l'image déjà chargée pour éviter le clignotement
                    currentWallpaperBitmap = _uiState.value.currentWallpaperBitmap,
                    isLoadingWallpaper = _uiState.value.isLoadingWallpaper
                )
            }.collect { newState ->
                // Si le type ou la valeur du wallpaper change, on recharge l'image
                if (newState.wallpaperType != _uiState.value.wallpaperType ||
                    newState.wallpaperValue != _uiState.value.wallpaperValue) {
                    loadWallpaper(newState.wallpaperType, newState.wallpaperValue)
                }
                _uiState.value = newState
            }
        }
    }

    private fun loadWallpaper(type: String, value: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWallpaper = true) }

            val bitmap = withContext(Dispatchers.IO) {
                try {
                    when (type) {
                        "URI" -> {
                            val uri = Uri.parse(value)
                            // On charge une version optimisée (max 720px de haut) pour la prévisualisation
                            decodeSampledBitmapFromUri(context, uri, 500, 720)?.asImageBitmap()
                        }
                        "RES" -> {
                            val resId = value.toIntOrNull()
                            if (resId != null) {
                                // On charge une version optimisée pour les ressources aussi
                                decodeSampledBitmapFromResource(context, resId, 500, 720)?.asImageBitmap()
                            } else null
                        }
                        else -> null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            _uiState.update { it.copy(currentWallpaperBitmap = bitmap, isLoadingWallpaper = false) }
        }
    }

    // --- FONCTIONS UTILITAIRES POUR RÉDUIRE LA TAILLE DES IMAGES (Anti-Crash) ---

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            // 1. On lit seulement les dimensions (sans charger l'image en mémoire)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            // 2. On calcule le facteur de réduction (sample size)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // 3. On lit l'image pour de vrai avec la réduction
            options.inJustDecodeBounds = false
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeSampledBitmapFromResource(context: Context, resId: Int, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            BitmapFactory.decodeResource(context.resources, resId, options)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // --- ACTIONS UTILISATEUR (CORRIGÉES POUR SettingsManager) ---

    fun setColumnCount(count: Int) = viewModelScope.launch { settingsManager.setColumnCount(count) }
    fun setRowCount(count: Int) = viewModelScope.launch { settingsManager.setRowCount(count) }
    fun setIconSize(size: Float) = viewModelScope.launch { settingsManager.setIconSize(size) }
    fun setFontSize(size: Float) = viewModelScope.launch { settingsManager.setFontSize(size) }
    fun setVerticalSpacing(spacing: Float) = viewModelScope.launch { settingsManager.setVerticalSpacing(spacing) }
    fun setShowLabels(show: Boolean) = viewModelScope.launch { settingsManager.setShowLabels(show) }
    fun setVerticalScroll(isVertical: Boolean) = viewModelScope.launch { settingsManager.setVerticalScroll(isVertical) }
    fun setAccentColor(color: Color) = viewModelScope.launch { settingsManager.setAccentColor(color.toArgb()) }

    // CORRECTION 1 : Le nom est setTransparency dans le manager
    fun setTransparency(enabled: Boolean) = viewModelScope.launch { settingsManager.setTransparency(enabled) }

    // CORRECTION 2 : Gestion de la liste pour les apps masquées
    fun setAppHidden(pkg: String, hidden: Boolean) = viewModelScope.launch {
        val currentHidden = _uiState.value.hiddenApps.toMutableSet()
        if (hidden) {
            currentHidden.add(pkg)
        } else {
            currentHidden.remove(pkg)
        }
        settingsManager.setHiddenApps(currentHidden)
    }

    // CORRECTION 3 : Appel des deux fonctions distinctes du manager
    fun setWallpaper(type: String, value: String) = viewModelScope.launch {
        settingsManager.setWallpaperType(type)
        settingsManager.setWallpaperValue(value)
    }
}